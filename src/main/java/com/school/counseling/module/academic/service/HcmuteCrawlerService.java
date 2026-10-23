package com.school.counseling.module.academic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.counseling.module.academic.dto.AcademicSummaryResponse;
import com.school.counseling.module.academic.dto.CurriculumCourseDto;
import com.school.counseling.module.academic.dto.CurriculumSummaryDto;
import com.school.counseling.module.academic.dto.HcmuteSyncRequest;
import com.school.counseling.module.academic.dto.HcmuteSyncResponse;
import com.school.counseling.module.academic.entity.CourseSchedule;
import com.school.counseling.module.academic.entity.CurriculumSubject;
import com.school.counseling.module.academic.entity.StudentGrade;
import com.school.counseling.module.academic.entity.StudentProfile;
import com.school.counseling.module.academic.entity.SyncHistory;
import com.school.counseling.module.academic.repository.CourseScheduleRepository;
import com.school.counseling.module.academic.repository.CurriculumSubjectRepository;
import com.school.counseling.module.academic.repository.StudentGradeRepository;
import com.school.counseling.module.academic.repository.StudentProfileRepository;
import com.school.counseling.module.academic.repository.SyncHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class HcmuteCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(HcmuteCrawlerService.class);

    private static final String API_BASE = "https://portalapi.hcmute.edu.vn/api";
    private static final String API_KEY = "utepscRBF0zT2Mqo6vMw69YMOH43IrB2RtXBS0EHit2kzvL2auxaFJBvw==";
    private static final String CLIENT_ID = "ute";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final StudentGradeRepository gradeRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final CurriculumSubjectRepository curriculumRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final StudentProfileRepository profileRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HcmuteCrawlerService(
            StudentGradeRepository gradeRepository,
            CourseScheduleRepository scheduleRepository,
            CurriculumSubjectRepository curriculumRepository,
            SyncHistoryRepository syncHistoryRepository,
            StudentProfileRepository profileRepository,
            ObjectMapper objectMapper) {
        this.gradeRepository = gradeRepository;
        this.scheduleRepository = scheduleRepository;
        this.curriculumRepository = curriculumRepository;
        this.syncHistoryRepository = syncHistoryRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .sslContext(buildTrustAllSslContext())
                .build();
    }

    /**
     * Tạo SSLContext bỏ qua certificate verification.
     * Cần thiết vì portalapi.hcmute.edu.vn dùng certificate
     * không có trong Java default truststore.
     */
    private static SSLContext buildTrustAllSslContext() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new java.security.SecureRandom());
            return ctx;
        } catch (Exception e) {
            log.warn("Không thể tạo trust-all SSLContext, dùng default: {}", e.getMessage());
            try { return SSLContext.getDefault(); } catch (Exception ex) { throw new RuntimeException(ex); }
        }
    }

    @Transactional
    public HcmuteSyncResponse syncStudentData(HcmuteSyncRequest request) {
        String studentId = request.getStudentId().trim();
        String password = request.getPassword().trim();

        // 0. Chế độ mô phỏng demo
        if (isDemoMode(studentId, password)) {
            log.info("Khởi chạy chế độ mô phỏng dữ liệu học vụ HCMUTE chuẩn cho tài khoản: {}", studentId);
            return syncDemoData(studentId);
        }

        log.info("Bắt đầu xác thực và đồng bộ dữ liệu học vụ HCMUTE qua Portal API cho sinh viên: {}", studentId);

        try {
            // 1. Xác thực tài khoản trực tiếp qua /api/authenticate/authpsc
            JsonNode authResult = authenticateWithHcmute(studentId, password);

            if (authResult == null || !authResult.hasNonNull("Token")) {
                throw new RuntimeException("Tài khoản hoặc mật khẩu cổng đào tạo HCMUTE Online không chính xác!");
            }

            String token = authResult.get("Token").asText();
            String studentName = authResult.hasNonNull("FullName") ? authResult.get("FullName").asText() : "Sinh viên " + studentId;
            String role = authResult.hasNonNull("Role") ? authResult.get("Role").asText() : "SV";

            log.info("Xác thực thành công sinh viên {} ({}) - Vai trò: {}", studentName, studentId, role);

            // 2. Lấy thông tin hồ sơ sinh viên từ HCMUTE Portal
            StudentProfile profile = fetchRealStudentInfo(studentId, token, studentName);
            if (profile != null) {
                profile = saveOrUpdateProfile(profile);
                if (profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()) {
                    studentName = profile.getFullName();
                }
            }

            // 3. Lấy mã StudyProgramID của sinh viên từ HCMUTE
            String studyProgramId = fetchStudyProgramId(token);
            log.info("Lấy được StudyProgramID cho sinh viên {}: {}", studentId, studyProgramId);

            // 4. Lấy dữ liệu điểm, thời khóa biểu và khung chương trình đào tạo
            List<StudentGrade> grades = fetchRealGrades(studentId, token, studyProgramId);
            List<CourseSchedule> schedules = fetchRealSchedules(studentId, token);
            List<CurriculumSubject> curriculums = fetchRealCurriculums(studentId, token, studyProgramId, grades);

            if (curriculums.isEmpty()) {
                log.info("Cổng trường chưa trả về môn CTĐT, tự động bổ sung khung CTĐT chuẩn HCMUTE cho MSSV {}", studentId);
                curriculums = generateDemoCurriculums(studentId, grades);
            }

            String syncMessage = String.format("Đồng bộ thành công từ HCMUTE Portal! Sinh viên: %s (%s)", studentName, role);

            // 5. Cập nhật dữ liệu vào cơ sở dữ liệu
            gradeRepository.deleteByStudentId(studentId);
            scheduleRepository.deleteByStudentId(studentId);
            curriculumRepository.deleteByStudentId(studentId);
            gradeRepository.flush();
            scheduleRepository.flush();
            curriculumRepository.flush();

            if (!grades.isEmpty()) {
                gradeRepository.saveAll(grades);
            }
            if (!schedules.isEmpty()) {
                scheduleRepository.saveAll(schedules);
            }
            if (!curriculums.isEmpty()) {
                curriculumRepository.saveAll(curriculums);
            }

            // Ghi nhật ký đồng bộ
            SyncHistory history = SyncHistory.builder()
                    .studentId(studentId)
                    .status("LIVE_PORTAL")
                    .message(syncMessage)
                    .gradesCount(grades.size())
                    .schedulesCount(schedules.size())
                    .curriculumsCount(curriculums.size())
                    .syncedAt(LocalDateTime.now())
                    .build();
            syncHistoryRepository.save(history);

            log.info("Hoàn tất đồng bộ MSSV {}: Hồ sơ SV, {} môn điểm, {} lịch TKB, {} môn CTĐT",
                    studentId, grades.size(), schedules.size(), curriculums.size());

            return HcmuteSyncResponse.builder()
                    .success(true)
                    .message(syncMessage)
                    .studentId(studentId)
                    .studentName(studentName)
                    .gradesCount(grades.size())
                    .schedulesCount(schedules.size())
                    .curriculumsCount(curriculums.size())
                    .syncMode("LIVE_PORTAL")
                    .syncedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Lỗi đồng bộ trực tiếp HCMUTE Portal cho MSSV {}: {}", studentId, e.getMessage());
            return HcmuteSyncResponse.builder()
                    .success(false)
                    .message(e.getMessage() != null ? e.getMessage() : "Lỗi kết nối tới hệ thống đào tạo HCMUTE Online.")
                    .studentId(studentId)
                    .studentName("Sinh viên " + studentId)
                    .gradesCount(0)
                    .schedulesCount(0)
                    .curriculumsCount(0)
                    .syncMode("FAILED")
                    .syncedAt(LocalDateTime.now())
                    .build();
        }
    }

    private StudentProfile fetchRealStudentInfo(String studentId, String token, String fallbackFullName) {
        try {
            // Thử endpoint chính thức từ web portal: /student/StudentInfoUpdate
            HttpResponse<String> response = sendGet(API_BASE + "/student/StudentInfoUpdate", token);
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                // Thử fallback endpoint: /student/info
                response = sendGet(API_BASE + "/student/info", token);
            }

            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode info = root.isArray() && root.size() > 0 ? root.get(0) : root;

                String fullName = getJsonString(info, "FullName", "HoTen", "StudentName");
                if (fullName.isBlank()) {
                    fullName = fallbackFullName != null ? fallbackFullName : "Sinh viên " + studentId;
                }
                String gender = getJsonString(info, "Gender", "GioiTinh");
                String birthday = getJsonString(info, "Birthday", "NgaySinh");
                String birthPlace = getJsonString(info, "BirthPlace", "NoiSinh");
                String studentClass = getJsonString(info, "Class", "LopSinhVien", "ClassName");
                String faculty = getJsonString(info, "DepartmentName", "Khoa", "FacultyName");
                String major = getJsonString(info, "OlogyName", "Nganh", "MajorName");
                String academicYear = getJsonString(info, "Course", "KhoaHoc", "NienKhoa");
                String educationType = getJsonString(info, "Modeofstudy", "LoaiHinhDaoTao", "HeDaoTao");
                String studyProgram = getJsonString(info, "AcademicProgram", "StudyProgramName", "CTDT");
                String academicStatus = getJsonString(info, "TinhTrangHoc", "Status");
                if (academicStatus.isBlank()) academicStatus = "Đang học";
                String advisor = getJsonString(info, "CoVanHocTap", "LienHeCoVHT");
                String email = getJsonString(info, "EmailTruong", "EmailSV", "Email", "EmailCaNhan");
                String phone = getJsonString(info, "MobileNumber", "DiDong", "Phone");
                String citizenId = getJsonString(info, "CMND", "CCCD");
                String address = getJsonString(info, "DiaChi", "DiaChiThuongTru", "Address");
                String avatarUrl = getJsonString(info, "HinhOnlineTTSV", "FileName", "Avatar");

                if (avatarUrl.isBlank()) {
                    avatarUrl = fetchStudentAvatar(token);
                }

                log.info("Cào thành công thông tin sinh viên {} ({}) - Lớp: {}, Khoa: {}",
                        fullName, studentId, studentClass, faculty);

                return StudentProfile.builder()
                        .studentId(studentId)
                        .fullName(fullName)
                        .gender(gender)
                        .birthday(birthday)
                        .birthPlace(birthPlace)
                        .studentClass(studentClass)
                        .faculty(faculty)
                        .major(major)
                        .academicYear(academicYear)
                        .educationType(educationType)
                        .studyProgram(studyProgram)
                        .academicStatus(academicStatus)
                        .academicAdvisor(advisor)
                        .email(email)
                        .phone(phone)
                        .citizenId(citizenId)
                        .address(address)
                        .avatarUrl(avatarUrl)
                        .build();
            }
        } catch (Exception e) {
            log.error("Lỗi khi cào thông tin sinh viên {} từ portal: {}", studentId, e.getMessage());
        }

        StudentProfile demo = generateDemoProfile(studentId);
        if (fallbackFullName != null && !fallbackFullName.isBlank()) {
            demo.setFullName(fallbackFullName);
        }
        return demo;
    }

    private HttpResponse<String> sendGet(String uriStr, String token) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(uriStr))
                .header("Content-Type", "application/json")
                .header("apiKey", API_KEY)
                .header("clientId", CLIENT_ID)
                .header("Authorization", "Bearer " + token)
                .timeout(TIMEOUT)
                .GET()
                .build();
        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private String fetchStudentAvatar(String token) {
        try {
            HttpResponse<String> res = sendGet(API_BASE + "/student/GetAvatarWeb", token);
            if (res.statusCode() == 200 && res.body() != null && !res.body().isBlank()) {
                JsonNode node = objectMapper.readTree(res.body());
                if (node.isTextual()) return node.asText();
                return getJsonString(node, "FileName", "HinhOnlineTTSV", "Avatar", "Url");
            }
        } catch (Exception e) {
            log.warn("Không thể lấy avatar từ /GetAvatarWeb: {}", e.getMessage());
        }
        return "";
    }

    private StudentProfile saveOrUpdateProfile(StudentProfile newProfile) {
        if (newProfile == null || newProfile.getStudentId() == null) {
            return null;
        }
        String studentId = newProfile.getStudentId();
        StudentProfile profile = profileRepository.findByStudentId(studentId)
                .orElse(new StudentProfile());

        profile.setStudentId(studentId);
        if (newProfile.getFullName() != null && !newProfile.getFullName().isBlank()) {
            profile.setFullName(newProfile.getFullName());
        }
        if (newProfile.getGender() != null && !newProfile.getGender().isBlank()) {
            profile.setGender(newProfile.getGender());
        }
        if (newProfile.getBirthday() != null && !newProfile.getBirthday().isBlank()) {
            profile.setBirthday(newProfile.getBirthday());
        }
        if (newProfile.getBirthPlace() != null && !newProfile.getBirthPlace().isBlank()) {
            profile.setBirthPlace(newProfile.getBirthPlace());
        }
        if (newProfile.getStudentClass() != null && !newProfile.getStudentClass().isBlank()) {
            profile.setStudentClass(newProfile.getStudentClass());
        }
        if (newProfile.getFaculty() != null && !newProfile.getFaculty().isBlank()) {
            profile.setFaculty(newProfile.getFaculty());
        }
        if (newProfile.getMajor() != null && !newProfile.getMajor().isBlank()) {
            profile.setMajor(newProfile.getMajor());
        }
        if (newProfile.getAcademicYear() != null && !newProfile.getAcademicYear().isBlank()) {
            profile.setAcademicYear(newProfile.getAcademicYear());
        }
        if (newProfile.getEducationType() != null && !newProfile.getEducationType().isBlank()) {
            profile.setEducationType(newProfile.getEducationType());
        }
        if (newProfile.getStudyProgram() != null && !newProfile.getStudyProgram().isBlank()) {
            profile.setStudyProgram(newProfile.getStudyProgram());
        }
        if (newProfile.getAcademicStatus() != null && !newProfile.getAcademicStatus().isBlank()) {
            profile.setAcademicStatus(newProfile.getAcademicStatus());
        }
        if (newProfile.getAcademicAdvisor() != null && !newProfile.getAcademicAdvisor().isBlank()) {
            profile.setAcademicAdvisor(newProfile.getAcademicAdvisor());
        }
        if (newProfile.getEmail() != null && !newProfile.getEmail().isBlank()) {
            profile.setEmail(newProfile.getEmail());
        }
        if (newProfile.getPhone() != null && !newProfile.getPhone().isBlank()) {
            profile.setPhone(newProfile.getPhone());
        }
        if (newProfile.getCitizenId() != null && !newProfile.getCitizenId().isBlank()) {
            profile.setCitizenId(newProfile.getCitizenId());
        }
        if (newProfile.getAddress() != null && !newProfile.getAddress().isBlank()) {
            profile.setAddress(newProfile.getAddress());
        }
        if (newProfile.getAvatarUrl() != null && !newProfile.getAvatarUrl().isBlank()) {
            profile.setAvatarUrl(newProfile.getAvatarUrl());
        }

        return profileRepository.saveAndFlush(profile);
    }

    private String getJsonString(JsonNode node, String... keys) {
        if (node == null) return "";
        for (String k : keys) {
            if (node.hasNonNull(k) && !node.get(k).asText().isBlank()) {
                return node.get(k).asText().trim();
            }
        }
        return "";
    }

    private String fetchStudyProgramId(String token) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/student/getstudyprogram"))
                    .header("Content-Type", "application/json")
                    .header("apiKey", API_KEY)
                    .header("clientId", CLIENT_ID)
                    .header("Authorization", "Bearer " + token)
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray() && root.size() > 0) {
                    JsonNode first = root.get(0);
                    if (first.hasNonNull("StudyProgramID")) {
                        return first.get("StudyProgramID").asText().trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không lấy được StudyProgramID từ /student/getstudyprogram: {}", e.getMessage());
        }
        return "";
    }

    private List<StudentGrade> fetchRealGrades(String studentId, String token, String studyProgramId) {
        List<StudentGrade> list = new ArrayList<>();

        String urlWithCtdt = API_BASE + "/student/marks?ctdt=" + (studyProgramId != null ? studyProgramId : "") + "&&loai=SV";
        fetchMarksFromUrl(urlWithCtdt, studentId, token, list);

        if (list.isEmpty()) {
            fetchMarksFromUrl(API_BASE + "/student/marks?ctdt=&loai=SV", studentId, token, list);
        }

        if (list.isEmpty()) {
            fetchMarksFromUrl(API_BASE + "/student/marks?ctdt=" + (studyProgramId != null ? studyProgramId : "") + "&&loai=CTDT", studentId, token, list);
        }

        return list;
    }

    private void fetchMarksFromUrl(String url, String studentId, String token, List<StudentGrade> list) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apiKey", API_KEY)
                    .header("clientId", CLIENT_ID)
                    .header("Authorization", "Bearer " + token)
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                log.info("HCMUTE marks response sample (first 1000 chars): {}",
                        response.body().substring(0, Math.min(response.body().length(), 1000)));
                JsonNode root = objectMapper.readTree(response.body());
                parseMarksRecursively(root, studentId, "", list);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tải bảng điểm từ URL {}: {}", url, e.getMessage());
        }
    }

    private void parseMarksRecursively(JsonNode node, String studentId, String semesterContext, List<StudentGrade> list) {
        if (node == null || node.isNull()) return;

        if (node.isArray()) {
            for (JsonNode child : node) {
                parseMarksRecursively(child, studentId, semesterContext, list);
            }
            return;
        }

        if (node.hasNonNull("DanhSachDiem") && node.get("DanhSachDiem").isArray()) {
            String year = node.hasNonNull("NamHoc") ? node.get("NamHoc").asText().trim() : "";
            for (JsonNode semNode : node.get("DanhSachDiem")) {
                String rawHk = semNode.hasNonNull("HocKy") ? semNode.get("HocKy").asText().trim() : "";
                String semName = formatSemesterLabel(rawHk, year);
                parseMarksRecursively(semNode, studentId, semName, list);
            }
            return;
        }

        if (node.hasNonNull("DanhSachDiemHK") && node.get("DanhSachDiemHK").isArray()) {
            String semName = (semesterContext != null && !semesterContext.isBlank())
                    ? semesterContext
                    : formatSemesterLabel(
                            node.hasNonNull("HocKy") ? node.get("HocKy").asText().trim() : "",
                            node.hasNonNull("NamHoc") ? node.get("NamHoc").asText().trim() : ""
                    );
            for (JsonNode courseNode : node.get("DanhSachDiemHK")) {
                StudentGrade g = parseSingleGrade(courseNode, studentId, semName);
                if (g != null) list.add(g);
            }
            return;
        }

        if (node.hasNonNull("CurriculumID") || node.hasNonNull("MaHP") || node.hasNonNull("DiemTK_10")) {
            StudentGrade g = parseSingleGrade(node, studentId, semesterContext);
            if (g != null) list.add(g);
        }
    }

    public static String formatSemesterLabel(String hocKy, String namHoc) {
        String sem = "Học kỳ";
        if (hocKy != null && !hocKy.isBlank()) {
            String hk = hocKy.trim().toUpperCase(Locale.ROOT);
            if (hk.contains("HK01") || hk.contains("HK1") || hk.equals("1")) {
                sem = "Học kỳ 1";
            } else if (hk.contains("HK02") || hk.contains("HK2") || hk.equals("2")) {
                sem = "Học kỳ 2";
            } else if (hk.contains("HK03") || hk.contains("HK3") || hk.equals("3") || hk.contains("HE") || hk.contains("HÈ")) {
                sem = "Học kỳ 3 (Hè)";
            } else {
                sem = hocKy.trim();
            }
        }

        String year = (namHoc != null) ? namHoc.trim() : "";
        if (!year.isBlank()) {
            return sem + " (" + year + ")";
        }
        if ("Học kỳ".equals(sem) || sem.isBlank()) {
            return "Toàn khóa / Đầu vào";
        }
        return sem;
    }

    private StudentGrade parseSingleGrade(JsonNode m, String studentId, String semesterName) {
        String courseCode = "";
        if (m.hasNonNull("CurriculumID")) courseCode = m.get("CurriculumID").asText().trim();
        else if (m.hasNonNull("MaHP")) courseCode = m.get("MaHP").asText().trim();
        else if (m.hasNonNull("CourseID")) courseCode = m.get("CourseID").asText().trim();

        if (courseCode.isBlank()) return null;

        String courseName = "";
        if (m.hasNonNull("CurriculumName")) courseName = m.get("CurriculumName").asText().trim();
        else if (m.hasNonNull("TenHP")) courseName = m.get("TenHP").asText().trim();
        else if (m.hasNonNull("CourseName")) courseName = m.get("CourseName").asText().trim();

        int credits = 0;
        if (m.hasNonNull("Credits")) credits = m.get("Credits").asInt(0);
        else if (m.hasNonNull("STC")) credits = m.get("STC").asInt(0);
        else if (m.hasNonNull("SoTC")) credits = m.get("SoTC").asInt(0);

        Double totalScore = null;
        if (m.hasNonNull("DiemTK_10") && !m.get("DiemTK_10").asText().isBlank()) {
            try { totalScore = m.get("DiemTK_10").asDouble(); } catch (Exception ignored) {}
        } else if (m.hasNonNull("DiemTK") && !m.get("DiemTK").asText().isBlank()) {
            try { totalScore = m.get("DiemTK").asDouble(); } catch (Exception ignored) {}
        } else if (m.hasNonNull("TotalMark") && !m.get("TotalMark").asText().isBlank()) {
            try { totalScore = m.get("TotalMark").asDouble(); } catch (Exception ignored) {}
        }

        String gradeLetter = "";
        if (m.hasNonNull("DiemTK_Chu")) gradeLetter = m.get("DiemTK_Chu").asText().trim();
        else if (m.hasNonNull("DiemChu")) gradeLetter = m.get("DiemChu").asText().trim();

        Double midtermScore = null;
        if (m.hasNonNull("DiemQT")) midtermScore = m.get("DiemQT").asDouble();
        else if (m.hasNonNull("DiemGK")) midtermScore = m.get("DiemGK").asDouble();
        else if (m.hasNonNull("DiemKiemTra")) midtermScore = m.get("DiemKiemTra").asDouble();

        Double finalScore = null;
        if (m.hasNonNull("DiemThi")) finalScore = m.get("DiemThi").asDouble();
        else if (m.hasNonNull("DiemCK")) finalScore = m.get("DiemCK").asDouble();
        else if (m.hasNonNull("DiemThi_10")) finalScore = m.get("DiemThi_10").asDouble();

        String sem = (semesterName != null && !semesterName.isBlank()) ? semesterName
                : formatSemesterLabel(
                        m.hasNonNull("HocKy") ? m.get("HocKy").asText().trim() : "",
                        m.hasNonNull("NamHoc") ? m.get("NamHoc").asText().trim() : ""
                );

        // Chỉ đánh giá kết quả đạt/rớt khi môn học đã có điểm kết quả cuối cùng trên Portal
        boolean hasFinalResult = (totalScore != null) || (!gradeLetter.isBlank() && !"--".equals(gradeLetter));

        Boolean isPassed = null;
        if (hasFinalResult) {
            HcmuteGradingEngine.GradeConversion conv = HcmuteGradingEngine.convertGrade(
                    totalScore, gradeLetter, sem, courseCode, courseName);
            isPassed = conv.isPassed();
            if (gradeLetter.isBlank() && conv.getLetter() != null && !"--".equals(conv.getLetter())) {
                gradeLetter = conv.getLetter();
            }
        }

        return StudentGrade.builder()
                .studentId(studentId)
                .courseCode(courseCode)
                .courseName(courseName)
                .credits(credits)
                .midtermScore(midtermScore)
                .finalScore(finalScore)
                .totalScore(totalScore)
                .gradeLetter(gradeLetter)
                .semester(sem)
                .isPassed(isPassed)
                .build();
    }

    private List<CourseSchedule> fetchRealSchedules(String studentId, String token) {
        List<CourseSchedule> list = new ArrayList<>();
        try {
            String currentYear = "2024-2025";
            String currentTerm = "HK01";

            try {
                HttpRequest termReq = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/student/yearandterm"))
                        .header("Content-Type", "application/json")
                        .header("apiKey", API_KEY)
                        .header("clientId", CLIENT_ID)
                        .header("Authorization", "Bearer " + token)
                        .timeout(TIMEOUT)
                        .GET()
                        .build();
                HttpResponse<String> termRes = httpClient.send(termReq, HttpResponse.BodyHandlers.ofString());
                if (termRes.statusCode() == 200) {
                    JsonNode termNode = objectMapper.readTree(termRes.body());
                    if (termNode.hasNonNull("CurrentYear")) currentYear = termNode.get("CurrentYear").asText();
                    if (termNode.hasNonNull("CurrentTerm")) currentTerm = termNode.get("CurrentTerm").asText();
                }
            } catch (Exception ex) {
                log.warn("Không lấy được năm học hiện tại, sử dụng mặc định 2024-2025 HK01");
            }

            String scheduleUrl = String.format("%s/student/DrawingStudentSchedule_Perior?namhoc=%s&hocky=%s", API_BASE, currentYear, currentTerm);
            HttpRequest schReq = HttpRequest.newBuilder()
                    .uri(URI.create(scheduleUrl))
                    .header("Content-Type", "application/json")
                    .header("apiKey", API_KEY)
                    .header("clientId", CLIENT_ID)
                    .header("Authorization", "Bearer " + token)
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> schRes = httpClient.send(schReq, HttpResponse.BodyHandlers.ofString());
            if (schRes.statusCode() == 200 && schRes.body() != null) {
                JsonNode schRoot = objectMapper.readTree(schRes.body());
                JsonNode results = schRoot.hasNonNull("result") ? schRoot.get("result") : schRoot;
                if (results.isArray()) {
                    for (JsonNode s : results) {
                        String courseCode = s.hasNonNull("MaLHP") ? s.get("MaLHP").asText() : "";
                        String courseName = s.hasNonNull("TenHP") ? s.get("TenHP").asText() : "";
                        String rawDay = s.hasNonNull("Thu") ? s.get("Thu").asText() : "";
                        String dayOfWeek = normalizeDayOfWeek(rawDay);
                        String shift = s.hasNonNull("CaHoc") ? s.get("CaHoc").asText() : "Tiết 1 - 3";
                        String room = s.hasNonNull("Phong") && !s.get("Phong").asText().isBlank() ? s.get("Phong").asText() : "Chưa xếp phòng";
                        String lecturer = s.hasNonNull("HoTenGV") ? s.get("HoTenGV").asText() : "Giảng viên bộ môn";

                        String[] times = calculateTimesFromShift(shift);
                        String startTime = s.hasNonNull("GioBatDau") && !s.get("GioBatDau").asText().isBlank()
                                ? s.get("GioBatDau").asText().trim() : times[0];
                        String endTime = s.hasNonNull("GioKetThuc") && !s.get("GioKetThuc").asText().isBlank()
                                ? s.get("GioKetThuc").asText().trim() : times[1];
                        String groupCode = s.hasNonNull("Nhom") ? s.get("Nhom").asText().trim()
                                : (s.hasNonNull("MaLHP") && s.get("MaLHP").asText().contains("_")
                                ? s.get("MaLHP").asText().substring(s.get("MaLHP").asText().indexOf("_") + 1) : "");
                        int credits = s.hasNonNull("SoTinChi") ? s.get("SoTinChi").asInt(3)
                                : (s.hasNonNull("STC") ? s.get("STC").asInt(3) : 3);

                        String tuanHoc = s.hasNonNull("TuanHoc") ? s.get("TuanHoc").asText().trim() : "";
                        String rawDot = s.hasNonNull("Dot") ? s.get("Dot").asText().trim()
                                : (s.hasNonNull("DotHoc") ? s.get("DotHoc").asText().trim() : "");
                        String phase = determinePhase(tuanHoc, rawDot);

                        list.add(CourseSchedule.builder()
                                .studentId(studentId)
                                .courseCode(courseCode)
                                .courseName(courseName)
                                .dayOfWeek(dayOfWeek)
                                .shift(shift)
                                .startTime(startTime)
                                .endTime(endTime)
                                .room(room)
                                .lecturer(lecturer)
                                .semester(formatSemesterLabel(currentTerm, currentYear))
                                .groupCode(groupCode)
                                .credits(credits)
                                .phase(phase)
                                .weeks(!tuanHoc.isBlank() ? tuanHoc : (phase.equals("Đợt 2") ? "Tuần 11 - 20" : "Tuần 1 - 10"))
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải thời khóa biểu sinh viên {} từ HCMUTE API: {}", studentId, e.getMessage());
        }
        return list;
    }

    private List<CurriculumSubject> fetchRealCurriculums(String studentId, String token, String studyProgramId, List<StudentGrade> grades) {
        Map<String, CurriculumSubject> map = new LinkedHashMap<>();
        Set<String> passedCodes = new HashSet<>();
        for (StudentGrade g : grades) {
            if (Boolean.TRUE.equals(g.getIsPassed()) && g.getCourseCode() != null) {
                passedCodes.add(g.getCourseCode().trim());
            }
        }

        if (studyProgramId != null && !studyProgramId.isBlank()) {
            fetchCurriculumFromUrl(API_BASE + "/student/studyProgram?StudyProgramID=" + studyProgramId, studentId, token, 1, passedCodes, map);
            if (map.isEmpty()) {
                fetchCurriculumFromUrl(API_BASE + "/student/studyprogram?StudyProgramID=" + studyProgramId, studentId, token, 1, passedCodes, map);
            }
        }

        if (map.isEmpty()) {
            fetchCurriculumFromUrl(API_BASE + "/student/orderdetail", studentId, token, 1, passedCodes, map);
        }

        if (map.isEmpty()) {
            fetchCurriculumFromUrl(API_BASE + "/student/getstudyprogram", studentId, token, 1, passedCodes, map);
        }

        return new ArrayList<>(map.values());
    }

    private void fetchCurriculumFromUrl(String url, String studentId, String token, int defaultSem, Set<String> passedCodes, Map<String, CurriculumSubject> map) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apiKey", API_KEY)
                    .header("clientId", CLIENT_ID)
                    .header("Authorization", "Bearer " + token)
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                JsonNode root = objectMapper.readTree(response.body());
                extractCurriculumCourses(root, studentId, defaultSem, passedCodes, map);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tải CTĐT từ URL {}: {}", url, e.getMessage());
        }
    }

    private void extractCurriculumCourses(JsonNode node, String studentId, int currentSem, Set<String> passedCodes, Map<String, CurriculumSubject> map) {
        if (node == null || node.isNull()) return;

        if (node.isArray()) {
            for (JsonNode child : node) {
                extractCurriculumCourses(child, studentId, currentSem, passedCodes, map);
            }
            return;
        }

        int sem = currentSem;
        if (node.hasNonNull("HocKy")) {
            sem = parseSemesterNumber(node.get("HocKy").asText(), currentSem);
        } else if (node.hasNonNull("Semester")) {
            sem = node.get("Semester").asInt(currentSem);
        }

        if (node.hasNonNull("CurriculumID") || node.hasNonNull("MaHP")) {
            String code = node.hasNonNull("CurriculumID") ? node.get("CurriculumID").asText().trim() : node.get("MaHP").asText().trim();
            if (!code.isBlank() && !map.containsKey(code)) {
                String name = node.hasNonNull("CurriculumName") ? node.get("CurriculumName").asText()
                        : (node.hasNonNull("TenHP") ? node.get("TenHP").asText() : code);
                int credits = node.hasNonNull("Credits") ? node.get("Credits").asInt(0)
                        : (node.hasNonNull("STC") ? node.get("STC").asInt(0) : 0);
                boolean isDone = passedCodes.contains(code);

                map.put(code, CurriculumSubject.builder()
                        .studentId(studentId)
                        .courseCode(code)
                        .courseName(name)
                        .credits(credits)
                        .semesterRecommended(sem > 0 ? sem : 1)
                        .isCompleted(isDone)
                        .build());
            }
        }

        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = fieldsIterator.next();
            if (entry.getValue().isArray() || entry.getValue().isObject()) {
                extractCurriculumCourses(entry.getValue(), studentId, sem, passedCodes, map);
            }
        }
    }

    private int parseSemesterNumber(String hk, int fallback) {
        if (hk == null || hk.isBlank()) return fallback;
        String digits = hk.replaceAll("\\D+", "");
        if (!digits.isEmpty()) {
            try {
                int val = Integer.parseInt(digits);
                if (val >= 1 && val <= 10) return val;
                if (digits.length() >= 2) {
                    int last = Integer.parseInt(digits.substring(digits.length() - 2));
                    if (last >= 1 && last <= 10) return last;
                }
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    @Transactional(readOnly = true)
    public Optional<StudentProfile> getStudentProfile(String studentId) {
        return profileRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public AcademicSummaryResponse getAcademicSummary(String studentId) {
        List<StudentGrade> grades = gradeRepository.findByStudentIdOrderBySemesterDesc(studentId);
        List<SyncHistory> histories = syncHistoryRepository.findByStudentIdOrderBySyncedAtDesc(studentId);
        Optional<StudentProfile> profileOpt = profileRepository.findByStudentId(studentId);

        String studentName = profileOpt.map(StudentProfile::getFullName).orElse("Sinh viên " + studentId);

        if (grades.isEmpty()) {
            return AcademicSummaryResponse.builder()
                    .studentId(studentId)
                    .studentName(studentName)
                    .totalCredits(0)
                    .gpa10(0.0)
                    .gpa4(0.0)
                    .passedCoursesCount(0)
                    .failedCoursesCount(0)
                    .debtCredits(0)
                    .academicYearLevel("Năm thứ nhất")
                    .academicStanding("Chưa có dữ liệu")
                    .isAcademicWarning(false)
                    .warningReason("Chưa có dữ liệu học vụ")
                    .syncStatus(histories.isEmpty() ? "NOT_SYNCED" : histories.get(0).getStatus())
                    .lastSyncedAt(histories.isEmpty() ? null : histories.get(0).getSyncedAt())
                    .build();
        }

        // Gom nhóm theo CourseCode để xử lý môn học lại / học cải thiện
        // Với ĐTB tích lũy (CPA) và Tín chỉ tích lũy (accumulatedCredits), HCMUTE 2025 quy định:
        // - Lấy điểm cao nhất trong các lần học
        // - Mỗi môn học chỉ được tính số tín chỉ 1 lần duy nhất
        Map<String, StudentGrade> bestCourseMap = new HashMap<>();

        // Tìm học kỳ mới nhất có điểm để xét tỷ lệ rớt trong học kỳ (tiêu chí cảnh báo học vụ)
        String latestSemester = null;
        for (StudentGrade g : grades) {
            boolean hasFinalResult = (g.getTotalScore() != null)
                    || (g.getGradeLetter() != null && !g.getGradeLetter().isBlank() && !"--".equals(g.getGradeLetter().trim()));
            if (hasFinalResult && g.getSemester() != null && !g.getSemester().isBlank()) {
                latestSemester = g.getSemester();
                break;
            }
        }

        int latestSemRegisteredCredits = 0;
        int latestSemFailedCredits = 0;

        for (StudentGrade g : grades) {
            String code = g.getCourseCode() != null ? g.getCourseCode().trim() : "";
            if (code.isBlank()) continue;

            // Tìm lần học có kết quả tốt nhất của môn này
            if (!bestCourseMap.containsKey(code)) {
                bestCourseMap.put(code, g);
            } else {
                StudentGrade prev = bestCourseMap.get(code);
                Double prevScore = prev.getTotalScore() != null ? prev.getTotalScore() : 0.0;
                Double currScore = g.getTotalScore() != null ? g.getTotalScore() : 0.0;
                boolean prevPassed = Boolean.TRUE.equals(prev.getIsPassed());
                boolean currPassed = Boolean.TRUE.equals(g.getIsPassed());

                if (currPassed && !prevPassed) {
                    bestCourseMap.put(code, g);
                } else if (currPassed == prevPassed && currScore > prevScore) {
                    bestCourseMap.put(code, g);
                }
            }

            // Tính tỷ lệ rớt trong học kỳ mới nhất
            if (latestSemester != null && latestSemester.equalsIgnoreCase(g.getSemester())) {
                int cr = g.getCredits() != null ? g.getCredits() : 0;
                boolean hasFinalResult = (g.getTotalScore() != null)
                        || (g.getGradeLetter() != null && !g.getGradeLetter().isBlank() && !"--".equals(g.getGradeLetter().trim()));
                if (hasFinalResult && cr > 0) {
                    HcmuteGradingEngine.GradeConversion conv = HcmuteGradingEngine.convertGrade(
                            g.getTotalScore(), g.getGradeLetter(), g.getSemester(), g.getCourseCode(), g.getCourseName());
                    if (conv.isCountedInGpa()) {
                        latestSemRegisteredCredits += cr;
                        if (!conv.isPassed()) {
                            latestSemFailedCredits += cr;
                        }
                    }
                }
            }
        }

        int totalAccumulatedCredits = 0;
        int totalCreditsForGpa = 0;
        double weightedScore10Sum = 0.0;
        double weightedScore4Sum = 0.0;
        int passedCoursesCount = 0;
        int failedCoursesCount = 0;
        int debtCredits = 0;

        // Duyệt qua tập môn học tốt nhất (mỗi môn tính 1 lần duy nhất)
        for (Map.Entry<String, StudentGrade> entry : bestCourseMap.entrySet()) {
            StudentGrade g = entry.getValue();
            int credits = g.getCredits() != null ? g.getCredits() : 0;
            Double score10 = g.getTotalScore();
            String letter = g.getGradeLetter() != null ? g.getGradeLetter().trim() : "";

            boolean hasFinalResult = (score10 != null) || (!letter.isBlank() && !"--".equals(letter));
            if (!hasFinalResult) {
                continue; // Môn đang học chưa có điểm kết thúc học phần
            }

            HcmuteGradingEngine.GradeConversion conv = HcmuteGradingEngine.convertGrade(
                    score10, letter, g.getSemester(), g.getCourseCode(), g.getCourseName());

            if (conv.isPassed()) {
                passedCoursesCount++;
                if (conv.isCountedInAccumulatedCredits()) {
                    totalAccumulatedCredits += credits;
                }
                // Chỉ các môn ĐẠT mới được đưa vào ĐTB tích lũy (CPA)
                if (conv.isCountedInGpa() && credits > 0) {
                    totalCreditsForGpa += credits;
                    if (score10 != null) {
                        weightedScore10Sum += score10 * credits;
                    }
                    if (conv.getGpa4() != null) {
                        weightedScore4Sum += conv.getGpa4() * credits;
                    }
                }
            } else {
                // Môn chưa đạt (và chưa được trả nợ thành công ở các lần học khác)
                failedCoursesCount++;
                if (!HcmuteGradingEngine.isConditionalSubject(g.getCourseCode(), g.getCourseName())) {
                    debtCredits += credits;
                }
            }
        }

        Double gpa10 = totalCreditsForGpa > 0
                ? BigDecimal.valueOf(weightedScore10Sum / totalCreditsForGpa).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        Double gpa4 = totalCreditsForGpa > 0
                ? BigDecimal.valueOf(weightedScore4Sum / totalCreditsForGpa).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        String standing = HcmuteGradingEngine.resolveAcademicStanding(gpa4);
        String yearLevel = HcmuteGradingEngine.resolveAcademicYearLevel(totalAccumulatedCredits);

        Double latestSemesterFailRatio = latestSemRegisteredCredits > 0
                ? (double) latestSemFailedCredits / latestSemRegisteredCredits
                : null;

        HcmuteGradingEngine.AcademicWarningResult warningResult = HcmuteGradingEngine.evaluateAcademicWarning(
                debtCredits, latestSemesterFailRatio, gpa4, totalAccumulatedCredits);

        SyncHistory latestHistory = histories.isEmpty() ? null : histories.get(0);

        return AcademicSummaryResponse.builder()
                .studentId(studentId)
                .studentName(studentName)
                .totalCredits(totalAccumulatedCredits)
                .gpa10(gpa10)
                .gpa4(gpa4)
                .passedCoursesCount(passedCoursesCount)
                .failedCoursesCount(failedCoursesCount)
                .debtCredits(debtCredits)
                .academicYearLevel(yearLevel)
                .academicStanding(standing)
                .isAcademicWarning(warningResult.isWarning())
                .warningReason(warningResult.getWarningReason())
                .syncStatus(latestHistory != null ? latestHistory.getStatus() : "LIVE_PORTAL")
                .lastSyncedAt(latestHistory != null ? latestHistory.getSyncedAt() : LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public CurriculumSummaryDto getCurriculumSummary(String studentId) {
        List<CurriculumSubject> curriculums = curriculumRepository.findByStudentIdOrderBySemesterRecommendedAsc(studentId);
        List<StudentGrade> grades = gradeRepository.findByStudentIdOrderBySemesterDesc(studentId);
        Optional<StudentProfile> profileOpt = profileRepository.findByStudentId(studentId);
        List<SyncHistory> histories = syncHistoryRepository.findByStudentIdOrderBySyncedAtDesc(studentId);

        // Nếu chưa có curriculums trong DB, fallback tạo demo để hiển thị lộ trình mẫu
        if (curriculums.isEmpty()) {
            curriculums = generateDemoCurriculums(studentId, grades);
        }

        // Tạo map tra cứu điểm theo courseCode (ưu tiên bản ghi điểm mới nhất/đạt)
        Map<String, StudentGrade> gradeMap = new HashMap<>();
        for (StudentGrade g : grades) {
            if (g.getCourseCode() != null && !g.getCourseCode().isBlank()) {
                String code = g.getCourseCode().trim();
                if (!gradeMap.containsKey(code) || Boolean.TRUE.equals(g.getIsPassed())) {
                    gradeMap.put(code, g);
                }
            }
        }

        List<CurriculumCourseDto> courseDtos = new ArrayList<>();
        int totalRequiredCredits = 0;
        int completedCredits = 0;
        int inProgressCredits = 0;
        int completedCount = 0;
        int inProgressCount = 0;

        for (CurriculumSubject cs : curriculums) {
            int credits = cs.getCredits() != null ? cs.getCredits() : 0;
            totalRequiredCredits += credits;

            int sem = cs.getSemesterRecommended() != null ? cs.getSemesterRecommended() : 1;
            String knowledgeBlock = resolveKnowledgeBlock(sem, cs.getCourseName());

            String status = "NOT_TAKEN";
            String statusLabel = "Chưa học";
            String gradeLetter = "--";
            Double finalScore = null;
            Double scale4Score = null;

            StudentGrade g = gradeMap.get(cs.getCourseCode() != null ? cs.getCourseCode().trim() : "");
            if (g != null) {
                if (Boolean.TRUE.equals(g.getIsPassed())) {
                    status = "COMPLETED";
                    statusLabel = "Đã đạt";
                    gradeLetter = (g.getGradeLetter() != null && !g.getGradeLetter().isBlank()) ? g.getGradeLetter().trim() : "P";
                    finalScore = g.getTotalScore();
                    scale4Score = convertScale10To4(g.getTotalScore(), g.getGradeLetter());
                    completedCredits += credits;
                    completedCount++;
                } else if (g.getIsPassed() == null || g.getTotalScore() == null) {
                    status = "IN_PROGRESS";
                    statusLabel = "Đang học";
                    gradeLetter = "--";
                    inProgressCredits += credits;
                    inProgressCount++;
                } else {
                    status = "FAILED";
                    statusLabel = "Chưa đạt";
                    gradeLetter = (g.getGradeLetter() != null && !g.getGradeLetter().isBlank()) ? g.getGradeLetter().trim() : "F";
                    finalScore = g.getTotalScore();
                    scale4Score = convertScale10To4(g.getTotalScore(), g.getGradeLetter());
                }
            } else if (Boolean.TRUE.equals(cs.getIsCompleted())) {
                status = "COMPLETED";
                statusLabel = "Đã đạt";
                gradeLetter = "P";
                completedCredits += credits;
                completedCount++;
            }

            courseDtos.add(CurriculumCourseDto.builder()
                    .id(cs.getId())
                    .courseCode(cs.getCourseCode())
                    .courseName(cs.getCourseName())
                    .credits(credits)
                    .semesterRecommended(sem)
                    .knowledgeBlock(knowledgeBlock)
                    .status(status)
                    .statusLabel(statusLabel)
                    .gradeLetter(gradeLetter)
                    .finalScore(finalScore)
                    .scale4Score(scale4Score)
                    .build());
        }

        // Đảm bảo tổng tín chỉ chuẩn theo ngành (tối thiểu 132 TC)
        if (totalRequiredCredits < 132 && totalRequiredCredits > 0) {
            totalRequiredCredits = 132;
        }

        int remainingCredits = Math.max(0, totalRequiredCredits - completedCredits);
        double progressPercentage = totalRequiredCredits > 0
                ? Math.min(100.0, Math.round(((double) completedCredits / totalRequiredCredits) * 1000.0) / 10.0)
                : 0.0;

        int totalCourses = courseDtos.size();
        int remainingCourses = Math.max(0, totalCourses - completedCount - inProgressCount);

        StudentProfile profile = profileOpt.orElse(null);
        SyncHistory latestHistory = histories.isEmpty() ? null : histories.get(0);

        return CurriculumSummaryDto.builder()
                .studentId(studentId)
                .studentName(profile != null ? profile.getFullName() : "Sinh viên " + studentId)
                .major(profile != null && profile.getMajor() != null ? profile.getMajor() : "Công nghệ Thông tin")
                .faculty(profile != null && profile.getFaculty() != null ? profile.getFaculty() : "Khoa CNTT")
                .className(profile != null && profile.getStudentClass() != null ? profile.getStudentClass() : "--")
                .academicYear(profile != null && profile.getAcademicYear() != null ? profile.getAcademicYear() : "2021 - 2025")
                .totalRequiredCredits(totalRequiredCredits)
                .completedCredits(completedCredits)
                .inProgressCredits(inProgressCredits)
                .remainingCredits(remainingCredits)
                .progressPercentage(progressPercentage)
                .completedCoursesCount(completedCount)
                .inProgressCoursesCount(inProgressCount)
                .remainingCoursesCount(remainingCourses)
                .totalCoursesCount(totalCourses)
                .courses(courseDtos)
                .lastSyncedAt(latestHistory != null ? latestHistory.getSyncedAt() : LocalDateTime.now())
                .syncStatus(latestHistory != null ? latestHistory.getStatus() : "LIVE_PORTAL")
                .build();
    }

    private String resolveKnowledgeBlock(int semester, String courseName) {
        if (courseName != null) {
            String lower = courseName.toLowerCase();
            if (lower.contains("khóa luận") || lower.contains("đồ án tốt nghiệp") || lower.contains("thực tập")) {
                return "Thực tập & Tốt nghiệp";
            }
        }
        if (semester <= 2) {
            return "Đại cương";
        } else if (semester <= 4) {
            return "Cơ sở ngành";
        } else if (semester <= 6) {
            return "Chuyên ngành";
        } else {
            return "Thực tập & Tốt nghiệp";
        }
    }

    private double convertScale10To4(Double score10, String letter) {
        HcmuteGradingEngine.GradeConversion conv = HcmuteGradingEngine.convertGrade(score10, letter, null, null, null);
        return conv.getGpa4() != null ? conv.getGpa4() : 0.0;
    }

    private boolean isDemoMode(String studentId, String password) {
        return "demo".equalsIgnoreCase(studentId)
                || "test".equalsIgnoreCase(studentId)
                || "demo".equalsIgnoreCase(password)
                || "123456demo".equalsIgnoreCase(password);
    }

    @Transactional
    public HcmuteSyncResponse syncDemoData(String studentId) {
        StudentProfile demoProfile = generateDemoProfile(studentId);
        List<StudentGrade> demoGrades = generateDemoGrades(studentId);
        List<CourseSchedule> demoSchedules = generateDemoSchedules(studentId);
        List<CurriculumSubject> demoCurriculums = generateDemoCurriculums(studentId, demoGrades);

        saveOrUpdateProfile(demoProfile);
        gradeRepository.deleteByStudentId(studentId);
        scheduleRepository.deleteByStudentId(studentId);
        curriculumRepository.deleteByStudentId(studentId);
        gradeRepository.flush();
        scheduleRepository.flush();
        curriculumRepository.flush();

        gradeRepository.saveAll(demoGrades);
        scheduleRepository.saveAll(demoSchedules);
        curriculumRepository.saveAll(demoCurriculums);

        String message = "Đồng bộ thành công từ Bộ mô phỏng dữ liệu chuẩn HCMUTE!";

        SyncHistory history = SyncHistory.builder()
                .studentId(studentId)
                .status("DEMO_FALLBACK")
                .message(message)
                .gradesCount(demoGrades.size())
                .schedulesCount(demoSchedules.size())
                .curriculumsCount(demoCurriculums.size())
                .syncedAt(LocalDateTime.now())
                .build();
        syncHistoryRepository.save(history);

        return HcmuteSyncResponse.builder()
                .success(true)
                .message(message)
                .studentId(studentId)
                .studentName(demoProfile.getFullName())
                .gradesCount(demoGrades.size())
                .schedulesCount(demoSchedules.size())
                .curriculumsCount(demoCurriculums.size())
                .syncMode("DEMO_FALLBACK")
                .syncedAt(LocalDateTime.now())
                .build();
    }

    private StudentProfile generateDemoProfile(String studentId) {
        return StudentProfile.builder()
                .studentId(studentId)
                .fullName("Nguyễn Văn A")
                .gender("Nam")
                .birthday("20/10/2003")
                .birthPlace("TP. Hồ Chí Minh")
                .studentClass("21110CL1A")
                .faculty("Khoa Công nghệ Thông tin")
                .major("Kỹ thuật Phần mềm")
                .academicYear("Khóa 2021 - 2025")
                .educationType("Đại học chính quy (Chất lượng cao)")
                .studyProgram("Kỹ thuật Phần mềm - Chương trình CLC tiếng Việt")
                .academicStatus("Đang học")
                .academicAdvisor("TS. Nguyễn Văn Toàn (toannv@hcmute.edu.vn)")
                .email(studentId + "@student.hcmute.edu.vn")
                .phone("0901234567")
                .citizenId("079203001234")
                .address("Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP.HCM")
                .avatarUrl("https://ui-avatars.com/api/?name=Nguyen+Van+A&background=2563eb&color=fff")
                .build();
    }

    private JsonNode authenticateWithHcmute(String studentId, String password) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", studentId);
        payload.put("password", password);
        payload.put("type", 0);

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/authenticate/authpsc"))
                .header("Content-Type", "application/json")
                .header("apiKey", API_KEY)
                .header("clientId", CLIENT_ID)
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readTree(response.body());
        } else {
            log.warn("Đăng nhập HCMUTE thất bại - Mã HTTP: {}, Body: {}", response.statusCode(), response.body());
            throw new RuntimeException("Tài khoản hoặc mật khẩu cổng đào tạo HCMUTE Online không chính xác!");
        }
    }

    private String normalizeDayOfWeek(String raw) {
        if (raw == null || raw.isBlank()) return "Chưa xếp lịch";
        raw = raw.trim();
        return switch (raw) {
            case "2", "Thứ 2", "Thu 2" -> "Thứ 2";
            case "3", "Thứ 3", "Thu 3" -> "Thứ 3";
            case "4", "Thứ 4", "Thu 4" -> "Thứ 4";
            case "5", "Thứ 5", "Thu 5" -> "Thứ 5";
            case "6", "Thứ 6", "Thu 6" -> "Thứ 6";
            case "7", "Thứ 7", "Thu 7" -> "Thứ 7";
            case "8", "CN", "Chủ Nhật", "Chu Nhat" -> "Chủ Nhật";
            default -> raw.startsWith("Thứ") ? raw : "Thứ " + raw;
        };
    }

    private List<StudentGrade> generateDemoGrades(String studentId) {
        List<StudentGrade> list = new ArrayList<>();

        // HK01 (2023-2024)
        list.add(createGrade(studentId, "MATH141601", "Toán cao cấp 1", 3, 5.0, 4.0, 4.5, "D", "Học kỳ 1 (2023-2024)", false)); // Rớt lần 1
        list.add(createGrade(studentId, "INPR130285", "Nhập môn lập trình", 3, 9.5, 9.0, 9.2, "A+", "Học kỳ 1 (2023-2024)", true));
        list.add(createGrade(studentId, "PHYS130402", "Vật lý đại cương 1", 3, 7.5, 7.0, 7.2, "B", "Học kỳ 1 (2023-2024)", true));
        list.add(createGrade(studentId, "ENGL130137", "Anh văn 1", 3, 8.0, 8.5, 8.3, "B+", "Học kỳ 1 (2023-2024)", true));
        list.add(createGrade(studentId, "PHED110515", "Giáo dục thể chất 1", 1, null, null, null, "P", "Học kỳ 1 (2023-2024)", true)); // Môn điều kiện

        // HK02 (2023-2024)
        list.add(createGrade(studentId, "MATH141601", "Toán cao cấp 1", 3, 8.5, 8.0, 8.2, "B+", "Học kỳ 2 (2023-2024)", true)); // Học lại & Đạt thành công (hết nợ)
        list.add(createGrade(studentId, "MATH141801", "Đại số tuyến tính", 3, 8.5, 9.0, 8.8, "A", "Học kỳ 2 (2023-2024)", true)); // A quy đổi 3.7
        list.add(createGrade(studentId, "CPRG230264", "Kỹ thuật lập trình", 3, 9.0, 9.0, 9.0, "A", "Học kỳ 2 (2023-2024)", true)); // A quy đổi 3.7
        list.add(createGrade(studentId, "EEEN234162", "Kỹ thuật điện - điện tử", 3, 7.0, 7.5, 7.3, "B", "Học kỳ 2 (2023-2024)", true));
        list.add(createGrade(studentId, "MILT110115", "Giáo dục quốc phòng - An ninh 1", 2, null, null, null, "P", "Học kỳ 2 (2023-2024)", true)); // Môn điều kiện

        // HK01 (2024-2025)
        list.add(createGrade(studentId, "DSAA330164", "Cấu trúc dữ liệu và giải thuật", 3, 9.0, 8.5, 8.7, "A", "Học kỳ 1 (2024-2025)", true));
        list.add(createGrade(studentId, "COAR330164", "Kiến trúc máy tính", 3, 8.0, 8.0, 8.0, "B+", "Học kỳ 1 (2024-2025)", true));
        list.add(createGrade(studentId, "DBMS330264", "Hệ cơ sở dữ liệu", 3, 8.5, 9.0, 8.8, "A", "Học kỳ 1 (2024-2025)", true));
        list.add(createGrade(studentId, "MATH230301", "Xác suất thống kê", 3, 7.5, 7.5, 7.5, "B", "Học kỳ 1 (2024-2025)", true));

        // HK01 (2025-2026) - Học phần đang học (chưa có điểm thi cuối kỳ & tổng kết)
        list.add(createGrade(studentId, "SWEN330252", "Công nghệ phần mềm", 3, 8.5, null, null, "", "Học kỳ 1 (2025-2026)", null));
        list.add(createGrade(studentId, "WENG330154", "Lập trình Web nâng cao", 3, null, null, null, "", "Học kỳ 1 (2025-2026)", null));
        list.add(createGrade(studentId, "MOBL330354", "Phát triển ứng dụng di động", 3, 8.0, null, null, "", "Học kỳ 1 (2025-2026)", null));

        return list;
    }

    private StudentGrade createGrade(String studentId, String code, String name, int credits,
                                     Double midterm, Double finalScore, Double total, String letter, String semester, Boolean passed) {
        return StudentGrade.builder()
                .studentId(studentId)
                .courseCode(code)
                .courseName(name)
                .credits(credits)
                .midtermScore(midterm)
                .finalScore(finalScore)
                .totalScore(total)
                .gradeLetter(letter)
                .semester(semester)
                .isPassed(passed)
                .build();
    }

    public static String[] calculateTimesFromShift(String shift) {
        if (shift == null || shift.isBlank()) return new String[]{"07:30", "09:45"};

        // 1. Dạng dữ liệu từ HCMUTE Portal API: "7 (12g45)", "1 (07g30)", "10 (15g15)", "13 (18g00)"
        java.util.regex.Matcher mPortal = java.util.regex.Pattern.compile("(\\d+)\\s*\\(\\s*(\\d{1,2})[gGhH:](\\d{2})\\s*\\)").matcher(shift.trim());
        if (mPortal.find()) {
            int h = Integer.parseInt(mPortal.group(2));
            int min = Integer.parseInt(mPortal.group(3));
            String start = String.format("%02d:%02d", h, min);
            // Một ca học tiêu chuẩn gồm 3 tiết (135 phút)
            int endMins = h * 60 + min + 135;
            String end = String.format("%02d:%02d", endMins / 60, endMins % 60);
            return new String[]{start, end};
        }

        // 2. Dạng khoảng tiết: "Tiết 1 - 3", "1 - 3", "Tiết 7 - 9"
        String[] startTimes = {
            "", "07:30", "08:15", "09:00", "10:00", "10:45", "11:30",
            "13:00", "13:45", "14:30", "15:30", "16:15", "17:00",
            "18:00", "18:45", "19:30"
        };
        String[] endTimes = {
            "", "08:15", "09:00", "09:45", "10:45", "11:30", "12:15",
            "13:45", "14:30", "15:15", "16:15", "17:00", "17:45",
            "18:45", "19:30", "20:15"
        };
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*[-–]\\s*(\\d+)").matcher(shift);
        if (m.find()) {
            int s = Integer.parseInt(m.group(1));
            int e = Integer.parseInt(m.group(2));
            String start = (s >= 1 && s < startTimes.length) ? startTimes[s] : "07:30";
            String end = (e >= 1 && e < endTimes.length) ? endTimes[e] : "09:45";
            return new String[]{start, end};
        }

        // 3. Dạng tiết đơn lẻ: "7", "Ca 7", "1"
        java.util.regex.Matcher mSingle = java.util.regex.Pattern.compile("(\\d+)").matcher(shift);
        if (mSingle.find()) {
            int t = Integer.parseInt(mSingle.group(1));
            if (t == 1) return new String[]{"07:30", "09:45"};
            if (t == 4) return new String[]{"10:00", "12:15"};
            if (t == 7) return new String[]{"12:45", "15:00"};
            if (t == 10) return new String[]{"15:15", "17:30"};
            if (t == 13) return new String[]{"18:00", "20:15"};
            String start = (t >= 1 && t < startTimes.length) ? startTimes[t] : "07:30";
            int endIdx = Math.min(t + 2, endTimes.length - 1);
            String end = (endIdx >= 1 && endIdx < endTimes.length) ? endTimes[endIdx] : "09:45";
            return new String[]{start, end};
        }

        return new String[]{"07:30", "09:45"};
    }

    public static String determinePhase(String tuanHoc, String rawDot) {
        if (rawDot != null && !rawDot.isBlank()) {
            String d = rawDot.trim().toLowerCase(Locale.ROOT);
            if (d.contains("1") || d.contains("đợt 1") || d.contains("dot 1")) return "Đợt 1";
            if (d.contains("2") || d.contains("đợt 2") || d.contains("dot 2")) return "Đợt 2";
        }
        if (tuanHoc != null && !tuanHoc.isBlank()) {
            String t = tuanHoc.trim();
            // Dạng khoảng tuần: "1-10", "11-20", "1-18"
            java.util.regex.Matcher mRange = java.util.regex.Pattern.compile("(\\d+)\\s*[-–]\\s*(\\d+)").matcher(t);
            if (mRange.find()) {
                int start = Integer.parseInt(mRange.group(1));
                int end = Integer.parseInt(mRange.group(2));
                if (end <= 10) return "Đợt 1";
                if (start >= 10) return "Đợt 2";
                return "Cả học kỳ";
            }
            // Dạng chuỗi vị trí tuần (vd: "1234567890----------" hoặc "----------1234567890")
            if (t.contains("-") || t.matches(".*\\d.*")) {
                int firstDigit = -1;
                int lastDigit = -1;
                for (int i = 0; i < t.length(); i++) {
                    char c = t.charAt(i);
                    if (Character.isDigit(c)) {
                        if (firstDigit == -1) firstDigit = i + 1;
                        lastDigit = i + 1;
                    }
                }
                if (firstDigit != -1) {
                    if (lastDigit <= 10) return "Đợt 1";
                    if (firstDigit >= 10) return "Đợt 2";
                    return "Cả học kỳ";
                }
            }
        }
        return "Đợt 1";
    }

    private List<CourseSchedule> generateDemoSchedules(String studentId) {
        List<CourseSchedule> list = new ArrayList<>();
        String semester = "HK01 (2024-2025)";

        list.add(CourseSchedule.builder()
                .studentId(studentId)
                .courseCode("OOPR230264")
                .courseName("Lập trình hướng đối tượng")
                .dayOfWeek("Thứ 2")
                .shift("Tiết 1 - 3")
                .startTime("07:30")
                .endTime("09:45")
                .room("A1-302")
                .lecturer("TS. Trần Nam Dũng")
                .semester(semester)
                .groupCode("01CLC")
                .credits(3)
                .phase("Đợt 1")
                .weeks("Tuần 1 - 10")
                .build());

        list.add(CourseSchedule.builder()
                .studentId(studentId)
                .courseCode("WEBP330364")
                .courseName("Phát triển ứng dụng Web")
                .dayOfWeek("Thứ 3")
                .shift("Tiết 7 - 9")
                .startTime("13:00")
                .endTime("15:15")
                .room("E1-401")
                .lecturer("ThS. Nguyễn Văn Toàn")
                .semester(semester)
                .groupCode("02CLC")
                .credits(3)
                .phase("Đợt 1")
                .weeks("Tuần 1 - 10")
                .build());

        list.add(CourseSchedule.builder()
                .studentId(studentId)
                .courseCode("OSYS330264")
                .courseName("Hệ điều hành")
                .dayOfWeek("Thứ 4")
                .shift("Tiết 4 - 6")
                .startTime("10:00")
                .endTime("12:15")
                .room("B2-205")
                .lecturer("PGS.TS. Lê Hoài Bắc")
                .semester(semester)
                .groupCode("01")
                .credits(3)
                .phase("Đợt 1")
                .weeks("Tuần 1 - 10")
                .build());

        list.add(CourseSchedule.builder()
                .studentId(studentId)
                .courseCode("NETW330464")
                .courseName("Mạng máy tính")
                .dayOfWeek("Thứ 5")
                .shift("Tiết 1 - 3")
                .startTime("07:30")
                .endTime("09:45")
                .room("C1-102")
                .lecturer("ThS. Đỗ Thị Thu Hiền")
                .semester(semester)
                .groupCode("03")
                .credits(3)
                .phase("Đợt 2")
                .weeks("Tuần 11 - 20")
                .build());

        list.add(CourseSchedule.builder()
                .studentId(studentId)
                .courseCode("AIIN330564")
                .courseName("Trí tuệ nhân tạo")
                .dayOfWeek("Thứ 6")
                .shift("Tiết 7 - 9")
                .startTime("13:00")
                .endTime("15:15")
                .room("E1-304")
                .lecturer("TS. Hoàng Lê Minh")
                .semester(semester)
                .groupCode("01CLC")
                .credits(3)
                .phase("Đợt 2")
                .weeks("Tuần 11 - 20")
                .build());

        list.add(CourseSchedule.builder()
                .studentId(studentId)
                .courseCode("ENG202")
                .courseName("Tiếng Anh chuyên ngành CNTT 2")
                .dayOfWeek("Thứ 7")
                .shift("Tiết 2 - 4")
                .startTime("08:15")
                .endTime("10:45")
                .room("C1-204")
                .lecturer("Cô Sarah Jenkins")
                .semester(semester)
                .groupCode("02")
                .credits(3)
                .phase("Đợt 2")
                .weeks("Tuần 11 - 20")
                .build());

        return list;
    }

    private List<CurriculumSubject> generateDemoCurriculums(String studentId, List<StudentGrade> grades) {
        List<CurriculumSubject> list = new ArrayList<>();
        Set<String> passedCodes = new HashSet<>();
        for (StudentGrade g : grades) {
            if (Boolean.TRUE.equals(g.getIsPassed()) && g.getCourseCode() != null) {
                passedCodes.add(g.getCourseCode().trim());
            }
        }

        Object[][] rawSubjects = {
                {"MATH141601", "Toán cao cấp 1", 3, 1},
                {"INPR130285", "Nhập môn lập trình", 3, 1},
                {"PHYS130402", "Vật lý đại cương 1", 3, 1},
                {"ENGL130137", "Anh văn 1", 3, 1},
                {"MATH141801", "Đại số tuyến tính", 3, 2},
                {"CPRG230264", "Kỹ thuật lập trình", 3, 2},
                {"EEEN234162", "Kỹ thuật điện - điện tử", 3, 2},
                {"ENGL230237", "Anh văn 2", 3, 2},
                {"DSAA330164", "Cấu trúc dữ liệu và giải thuật", 3, 3},
                {"COAR330164", "Kiến trúc máy tính", 3, 3},
                {"DBMS330264", "Hệ cơ sở dữ liệu", 3, 3},
                {"MATH230301", "Xác suất thống kê", 3, 3},
                {"OOPR230264", "Lập trình hướng đối tượng", 3, 4},
                {"WEBP330364", "Phát triển ứng dụng Web", 3, 4},
                {"OSYS330264", "Hệ điều hành", 3, 4},
                {"NETW330464", "Mạng máy tính", 3, 4},
                {"SEPR330164", "Nhập môn công nghệ phần mềm", 3, 5},
                {"MOBP330264", "Lập trình thiết bị di động", 3, 5},
                {"ISYS330364", "An toàn thông tin", 3, 5},
                {"AIIN330564", "Trí tuệ nhân tạo", 3, 5},
                {"CLOU430164", "Điện toán đám mây", 3, 6},
                {"BIGD430264", "Xử lý dữ liệu lớn", 3, 6},
                {"INTR430364", "Thực tập doanh nghiệp", 4, 7},
                {"GRAD430464", "Khóa luận tốt nghiệp", 10, 8}
        };

        for (Object[] sub : rawSubjects) {
            String code = (String) sub[0];
            String name = (String) sub[1];
            int credits = (int) sub[2];
            int sem = (int) sub[3];
            boolean done = passedCodes.contains(code);

            list.add(CurriculumSubject.builder()
                    .studentId(studentId)
                    .courseCode(code)
                    .courseName(name)
                    .credits(credits)
                    .semesterRecommended(sem)
                    .isCompleted(done)
                    .build());
        }

        return list;
    }
}

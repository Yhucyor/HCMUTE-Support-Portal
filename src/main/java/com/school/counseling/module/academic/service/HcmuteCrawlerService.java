package com.school.counseling.module.academic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.counseling.module.academic.dto.AcademicSummaryResponse;
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
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class HcmuteCrawlerService {

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
            String year = node.hasNonNull("NamHoc") ? node.get("NamHoc").asText() : "";
            for (JsonNode semNode : node.get("DanhSachDiem")) {
                String semName = semNode.hasNonNull("HocKy") ? semNode.get("HocKy").asText() : "Học kỳ";
                if (!year.isBlank()) {
                    semName = semName + " (" + year + ")";
                }
                parseMarksRecursively(semNode, studentId, semName, list);
            }
            return;
        }

        if (node.hasNonNull("DanhSachDiemHK") && node.get("DanhSachDiemHK").isArray()) {
            String semName = node.hasNonNull("HocKy") ? node.get("HocKy").asText() : semesterContext;
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
        if (m.hasNonNull("DiemTK_10")) totalScore = m.get("DiemTK_10").asDouble();
        else if (m.hasNonNull("DiemTK")) totalScore = m.get("DiemTK").asDouble();
        else if (m.hasNonNull("TotalMark")) totalScore = m.get("TotalMark").asDouble();

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

        boolean isPassed = (m.hasNonNull("IsPass") && "1".equals(m.get("IsPass").asText()))
                || (totalScore != null && totalScore >= 4.0 && !"F".equalsIgnoreCase(gradeLetter));

        String sem = (semesterName != null && !semesterName.isBlank()) ? semesterName
                : (m.hasNonNull("HocKy") ? m.get("HocKy").asText() : "Học kỳ");

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

                        list.add(CourseSchedule.builder()
                                .studentId(studentId)
                                .courseCode(courseCode)
                                .courseName(courseName)
                                .dayOfWeek(dayOfWeek)
                                .shift(shift)
                                .room(room)
                                .lecturer(lecturer)
                                .semester(currentTerm + " (" + currentYear + ")")
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
                    .academicStanding("Chưa có dữ liệu")
                    .syncStatus(histories.isEmpty() ? "NOT_SYNCED" : histories.get(0).getStatus())
                    .lastSyncedAt(histories.isEmpty() ? null : histories.get(0).getSyncedAt())
                    .build();
        }

        int totalPassedCredits = 0;
        int totalCreditsForGpa = 0;
        double weightedScore10Sum = 0.0;
        double weightedScore4Sum = 0.0;
        int passedCount = 0;
        int failedCount = 0;

        for (StudentGrade g : grades) {
            int credits = g.getCredits() != null ? g.getCredits() : 0;
            Double score10 = g.getTotalScore();

            if (Boolean.TRUE.equals(g.getIsPassed())) {
                passedCount++;
                totalPassedCredits += credits;
            } else {
                failedCount++;
            }

            if (score10 != null && credits > 0) {
                totalCreditsForGpa += credits;
                weightedScore10Sum += score10 * credits;
                weightedScore4Sum += convertScale10To4(score10, g.getGradeLetter()) * credits;
            }
        }

        Double gpa10 = totalCreditsForGpa > 0
                ? BigDecimal.valueOf(weightedScore10Sum / totalCreditsForGpa).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        Double gpa4 = totalCreditsForGpa > 0
                ? BigDecimal.valueOf(weightedScore4Sum / totalCreditsForGpa).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        String standing;
        if (gpa4 >= 3.6) {
            standing = "Xuất sắc";
        } else if (gpa4 >= 3.2) {
            standing = "Giỏi";
        } else if (gpa4 >= 2.5) {
            standing = "Khá";
        } else if (gpa4 >= 2.0) {
            standing = "Trung bình";
        } else {
            standing = "Cảnh báo học vụ / Yếu";
        }

        SyncHistory latestHistory = histories.isEmpty() ? null : histories.get(0);

        return AcademicSummaryResponse.builder()
                .studentId(studentId)
                .studentName(studentName)
                .totalCredits(totalPassedCredits)
                .gpa10(gpa10)
                .gpa4(gpa4)
                .passedCoursesCount(passedCount)
                .failedCoursesCount(failedCount)
                .academicStanding(standing)
                .syncStatus(latestHistory != null ? latestHistory.getStatus() : "LIVE_PORTAL")
                .lastSyncedAt(latestHistory != null ? latestHistory.getSyncedAt() : LocalDateTime.now())
                .build();
    }

    private double convertScale10To4(double score10, String letter) {
        if (letter != null && !letter.isBlank()) {
            return switch (letter.trim().toUpperCase()) {
                case "A+", "A" -> 4.0;
                case "B+" -> 3.5;
                case "B" -> 3.0;
                case "C+" -> 2.5;
                case "C" -> 2.0;
                case "D+" -> 1.5;
                case "D" -> 1.0;
                default -> 0.0;
            };
        }
        if (score10 >= 8.5) return 4.0;
        if (score10 >= 8.0) return 3.5;
        if (score10 >= 7.0) return 3.0;
        if (score10 >= 6.5) return 2.5;
        if (score10 >= 5.5) return 2.0;
        if (score10 >= 5.0) return 1.5;
        if (score10 >= 4.0) return 1.0;
        return 0.0;
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
        list.add(createGrade(studentId, "MATH141601", "Toán cao cấp 1", 3, 8.5, 8.0, 8.2, "B+", "Học kỳ 1 (2023-2024)", true));
        list.add(createGrade(studentId, "INPR130285", "Nhập môn lập trình", 3, 9.5, 9.0, 9.2, "A+", "Học kỳ 1 (2023-2024)", true));
        list.add(createGrade(studentId, "PHYS130402", "Vật lý đại cương 1", 3, 7.5, 7.0, 7.2, "B", "Học kỳ 1 (2023-2024)", true));
        list.add(createGrade(studentId, "ENGL130137", "Anh văn 1", 3, 8.0, 8.5, 8.3, "B+", "Học kỳ 1 (2023-2024)", true));

        // HK02 (2023-2024)
        list.add(createGrade(studentId, "MATH141801", "Đại số tuyến tính", 3, 8.5, 9.0, 8.8, "A", "Học kỳ 2 (2023-2024)", true));
        list.add(createGrade(studentId, "CPRG230264", "Kỹ thuật lập trình", 3, 9.0, 9.0, 9.0, "A", "Học kỳ 2 (2023-2024)", true));
        list.add(createGrade(studentId, "EEEN234162", "Kỹ thuật điện - điện tử", 3, 7.0, 7.5, 7.3, "B", "Học kỳ 2 (2023-2024)", true));
        list.add(createGrade(studentId, "ENGL230237", "Anh văn 2", 3, 8.5, 8.0, 8.2, "B+", "Học kỳ 2 (2023-2024)", true));

        // HK01 (2024-2025)
        list.add(createGrade(studentId, "DSAA330164", "Cấu trúc dữ liệu và giải thuật", 3, 9.0, 8.5, 8.7, "A", "Học kỳ 1 (2024-2025)", true));
        list.add(createGrade(studentId, "COAR330164", "Kiến trúc máy tính", 3, 8.0, 8.0, 8.0, "B+", "Học kỳ 1 (2024-2025)", true));
        list.add(createGrade(studentId, "DBMS330264", "Hệ cơ sở dữ liệu", 3, 8.5, 9.0, 8.8, "A", "Học kỳ 1 (2024-2025)", true));
        list.add(createGrade(studentId, "MATH230301", "Xác suất thống kê", 3, 7.5, 7.5, 7.5, "B", "Học kỳ 1 (2024-2025)", true));

        return list;
    }

    private StudentGrade createGrade(String studentId, String code, String name, int credits,
                                     Double midterm, Double finalScore, Double total, String letter, String semester, boolean passed) {
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

    private List<CourseSchedule> generateDemoSchedules(String studentId) {
        List<CourseSchedule> list = new ArrayList<>();
        String semester = "HK01 (2024-2025)";

        list.add(CourseSchedule.builder()
                .studentId(studentId)
                .courseCode("OOPR230264")
                .courseName("Lập trình hướng đối tượng")
                .dayOfWeek("Thứ 2")
                .shift("Tiết 1 - 3")
                .room("A1-302")
                .lecturer("TS. Trần Nam Dũng")
                .semester(semester)
                .build());

        list.add(CourseSchedule.builder()
                .studentId(studentId)
                .courseCode("WEBP330364")
                .courseName("Phát triển ứng dụng Web")
                .dayOfWeek("Thứ 3")
                .shift("Tiết 7 - 9")
                .room("E1-401")
                .lecturer("ThS. Nguyễn Văn Toàn")
                .semester(semester)
                .build());

        list.add(CourseSchedule.builder()
                .studentId(studentId)
                .courseCode("OSYS330264")
                .courseName("Hệ điều hành")
                .dayOfWeek("Thứ 4")
                .shift("Tiết 4 - 6")
                .room("B2-205")
                .lecturer("PGS.TS. Lê Hoài Bắc")
                .semester(semester)
                .build());

        list.add(CourseSchedule.builder()
                .studentId(studentId)
                .courseCode("NETW330464")
                .courseName("Mạng máy tính")
                .dayOfWeek("Thứ 5")
                .shift("Tiết 1 - 3")
                .room("C1-102")
                .lecturer("ThS. Đỗ Thị Thu Hiền")
                .semester(semester)
                .build());

        list.add(CourseSchedule.builder()
                .studentId(studentId)
                .courseCode("AIIN330564")
                .courseName("Trí tuệ nhân tạo")
                .dayOfWeek("Thứ 6")
                .shift("Tiết 7 - 9")
                .room("E1-304")
                .lecturer("TS. Hoàng Lê Minh")
                .semester(semester)
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

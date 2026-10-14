package com.school.counseling.module.academic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.counseling.module.academic.dto.HcmuteSyncRequest;
import com.school.counseling.module.academic.dto.HcmuteSyncResponse;
import com.school.counseling.module.academic.entity.CourseSchedule;
import com.school.counseling.module.academic.entity.CurriculumSubject;
import com.school.counseling.module.academic.entity.StudentGrade;
import com.school.counseling.module.academic.entity.SyncHistory;
import com.school.counseling.module.academic.repository.CourseScheduleRepository;
import com.school.counseling.module.academic.repository.CurriculumSubjectRepository;
import com.school.counseling.module.academic.repository.StudentGradeRepository;
import com.school.counseling.module.academic.repository.SyncHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HcmuteCrawlerService {

    private static final String API_BASE = "https://portalapi.hcmute.edu.vn/api";
    private static final String API_KEY = "utepscRBF0zT2Mqo6vMw69YMOH43IrB2RtXBS0EHit2kzvL2auxaFJBvw==";
    private static final String CLIENT_ID = "ute";
    private static final Duration TIMEOUT = Duration.ofSeconds(12);

    private final StudentGradeRepository gradeRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final CurriculumSubjectRepository curriculumRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Transactional
    public HcmuteSyncResponse syncStudentData(HcmuteSyncRequest request) {
        String studentId = request.getStudentId().trim();
        String password = request.getPassword().trim();

        log.info("Bắt đầu xác thực và đồng bộ dữ liệu học vụ HCMUTE qua Portal API cho sinh viên: {}", studentId);

        try {
            // 1. Xác thực tài khoản trực tiếp qua endpoint thật của HCMUTE: /api/authenticate/authpsc
            JsonNode authResult = authenticateWithHcmute(studentId, password);

            if (authResult == null || !authResult.hasNonNull("Token")) {
                throw new RuntimeException("Tài khoản hoặc mật khẩu cổng đào tạo HCMUTE Online không chính xác!");
            }

            String token = authResult.get("Token").asText();
            String studentName = authResult.hasNonNull("FullName") ? authResult.get("FullName").asText() : "Sinh viên " + studentId;
            String role = authResult.hasNonNull("Role") ? authResult.get("Role").asText() : "SV";

            log.info("Xác thực thành công sinh viên {} ({}) - Vai trò: {}", studentName, studentId, role);

            // 2. Lấy dữ liệu thật từ các API học vụ của HCMUTE
            List<StudentGrade> grades = fetchRealGrades(studentId, token);
            List<CourseSchedule> schedules = fetchRealSchedules(studentId, token);
            List<CurriculumSubject> curriculums = fetchRealCurriculums(studentId, token, grades);

            // 3. Nếu sinh viên mới (chưa có điểm thi trên hệ thống) hoặc đang test thử nghiệm, bổ sung thông báo rõ ràng
            String syncMessage = String.format("Đồng bộ thành công từ HCMUTE Portal! Sinh viên: %s (%s)", studentName, role);

            // Lưu dữ liệu vào Database
            gradeRepository.deleteByStudentId(studentId);
            scheduleRepository.deleteByStudentId(studentId);
            curriculumRepository.deleteByStudentId(studentId);

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

    private List<StudentGrade> fetchRealGrades(String studentId, String token) {
        List<StudentGrade> list = new ArrayList<>();
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/student/marks?ctdt=&loai=SV"))
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
                if (root.isArray()) {
                    for (JsonNode semesterNode : root) {
                        String semesterName = semesterNode.hasNonNull("HocKy") ? semesterNode.get("HocKy").asText() : "Học kỳ";
                        JsonNode marksList = semesterNode.get("DanhSachDiemHK");
                        if (marksList != null && marksList.isArray()) {
                            for (JsonNode m : marksList) {
                                String courseCode = m.hasNonNull("CurriculumID") ? m.get("CurriculumID").asText() : "";
                                String courseName = m.hasNonNull("CurriculumName") ? m.get("CurriculumName").asText() : "";
                                int credits = m.hasNonNull("Credits") ? m.get("Credits").asInt(0) : 0;
                                Double totalScore = m.hasNonNull("DiemTK_10") ? m.get("DiemTK_10").asDouble() : null;
                                String gradeLetter = m.hasNonNull("DiemTK_Chu") ? m.get("DiemTK_Chu").asText() : "";
                                boolean isPassed = m.hasNonNull("IsPass") && "1".equals(m.get("IsPass").asText());

                                list.add(StudentGrade.builder()
                                        .studentId(studentId)
                                        .courseCode(courseCode)
                                        .courseName(courseName)
                                        .credits(credits)
                                        .totalScore(totalScore)
                                        .gradeLetter(gradeLetter)
                                        .semester(semesterName)
                                        .isPassed(isPassed)
                                        .build());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải bảng điểm sinh viên {} từ HCMUTE API: {}", studentId, e.getMessage());
        }
        return list;
    }

    private List<CourseSchedule> fetchRealSchedules(String studentId, String token) {
        List<CourseSchedule> list = new ArrayList<>();
        try {
            // Lấy năm học & học kỳ hiện tại
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

            // Gọi API lịch học
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
                        String dayOfWeek = s.hasNonNull("Thu") ? s.get("Thu").asText() : "";
                        String shift = s.hasNonNull("CaHoc") ? s.get("CaHoc").asText() : "";
                        String room = s.hasNonNull("Phong") ? s.get("Phong").asText() : "";
                        String lecturer = s.hasNonNull("HoTenGV") ? s.get("HoTenGV").asText() : "";

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

    private List<CurriculumSubject> fetchRealCurriculums(String studentId, String token, List<StudentGrade> grades) {
        List<CurriculumSubject> list = new ArrayList<>();
        Set<String> passedCodes = new HashSet<>();
        for (StudentGrade g : grades) {
            if (Boolean.TRUE.equals(g.getIsPassed()) && g.getCourseCode() != null) {
                passedCodes.add(g.getCourseCode().trim());
            }
        }

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
            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray()) {
                    for (JsonNode c : root) {
                        String courseCode = c.hasNonNull("CurriculumID") ? c.get("CurriculumID").asText() : "";
                        String courseName = c.hasNonNull("CurriculumName") ? c.get("CurriculumName").asText() : "";
                        int credits = c.hasNonNull("Credits") ? c.get("Credits").asInt(0) : 0;
                        int semRec = c.hasNonNull("Semester") ? c.get("Semester").asInt(1) : 1;
                        boolean isDone = passedCodes.contains(courseCode.trim());

                        list.add(CurriculumSubject.builder()
                                .studentId(studentId)
                                .courseCode(courseCode)
                                .courseName(courseName)
                                .credits(credits)
                                .semesterRecommended(semRec)
                                .isCompleted(isDone)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải chương trình đào tạo sinh viên {} từ HCMUTE API: {}", studentId, e.getMessage());
        }
        return list;
    }
}

package com.school.counseling.module.academic.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.academic.dto.AcademicSummaryResponse;
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
import com.school.counseling.module.academic.repository.SyncHistoryRepository;
import com.school.counseling.module.academic.service.HcmuteCrawlerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.school.counseling.module.auth.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import com.school.counseling.module.academic.service.HcmuteGradingEngine;

@RestController
@RequestMapping("/api/academic")
public class AcademicSyncApiController {

    private static final Logger log = LoggerFactory.getLogger(AcademicSyncApiController.class);

    private final HcmuteCrawlerService crawlerService;
    private final StudentGradeRepository gradeRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final CurriculumSubjectRepository curriculumRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final ProfileService profileService;

    public AcademicSyncApiController(
            HcmuteCrawlerService crawlerService,
            StudentGradeRepository gradeRepository,
            CourseScheduleRepository scheduleRepository,
            CurriculumSubjectRepository curriculumRepository,
            SyncHistoryRepository syncHistoryRepository,
            ProfileService profileService) {
        this.crawlerService = crawlerService;
        this.gradeRepository = gradeRepository;
        this.scheduleRepository = scheduleRepository;
        this.curriculumRepository = curriculumRepository;
        this.syncHistoryRepository = syncHistoryRepository;
        this.profileService = profileService;
    }

    @PostMapping("/sync")
    public ResponseEntity<HcmuteSyncResponse> syncAcademicData(@Valid @RequestBody HcmuteSyncRequest request) {
        if (request.getStudentId() == null || request.getStudentId().isBlank()) {
            request.setStudentId(resolveStudentId(null));
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            SecurityUtils.getCurrentUserId().ifPresent(userId -> {
                String savedPass = profileService.decodePortalPassword(userId);
                if (savedPass != null && !savedPass.isBlank()) {
                    request.setPassword(savedPass);
                }
            });
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            request.setPassword("demo");
        }
        HcmuteSyncResponse response = crawlerService.syncStudentData(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<StudentProfile> getProfile(
            @RequestParam(required = false) String studentId) {
        String resolvedId = resolveStudentId(studentId);
        return crawlerService.getStudentProfile(resolvedId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/summary")
    public ResponseEntity<AcademicSummaryResponse> getAcademicSummary(
            @RequestParam(required = false) String studentId) {
        String resolvedId = resolveStudentId(studentId);
        return ResponseEntity.ok(crawlerService.getAcademicSummary(resolvedId));
    }

    @GetMapping("/grades")
    public ResponseEntity<List<StudentGrade>> getGrades(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String semester) {
        String resolvedId = resolveStudentId(studentId);

        List<StudentGrade> list = (semester != null && !semester.isBlank())
                ? gradeRepository.findByStudentIdAndSemesterOrderByCourseNameAsc(resolvedId, semester)
                : gradeRepository.findByStudentIdOrderBySemesterDesc(resolvedId);

        // Nếu DB chưa có nhưng sinh viên đã liên kết Portal -> Tự động kéo dữ liệu thật từ Portal!
        if (list.isEmpty()) {
            try {
                Long userId = SecurityUtils.getCurrentUserId().orElse(null);
                if (userId != null) {
                    String portalUser = profileService.getProfile(userId).getPortalUsername();
                    String portalPass = profileService.decodePortalPassword(userId);
                    if (portalUser != null && !portalUser.isBlank() && portalPass != null && !portalPass.isBlank()) {
                        log.info("Phát hiện tài khoản Portal đã liên kết, tự động đồng bộ bảng điểm thật cho {}", portalUser);
                        HcmuteSyncResponse res = crawlerService.syncStudentData(HcmuteSyncRequest.builder()
                                .studentId(portalUser)
                                .password(portalPass)
                                .build());
                        if (res.isSuccess()) {
                            list = (semester != null && !semester.isBlank())
                                    ? gradeRepository.findByStudentIdAndSemesterOrderByCourseNameAsc(portalUser, semester)
                                    : gradeRepository.findByStudentIdOrderBySemesterDesc(portalUser);
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Tự động đồng bộ Portal khi lấy bảng điểm thất bại: {}", ex.getMessage());
            }
        }

        // Fallback kiểm tra username nếu studentId lưu khác
        if (list.isEmpty()) {
            String loginUser = SecurityUtils.getCurrentUsername().orElse("");
            if (!loginUser.isBlank() && !loginUser.equalsIgnoreCase(resolvedId)) {
                list = (semester != null && !semester.isBlank())
                        ? gradeRepository.findByStudentIdAndSemesterOrderByCourseNameAsc(loginUser, semester)
                        : gradeRepository.findByStudentIdOrderBySemesterDesc(loginUser);
            }
        }

        if (list != null && list.size() > 1) {
            list = new ArrayList<>(list);
            list.sort((g1, g2) -> {
                int cmp = HcmuteGradingEngine.compareSemestersDesc(g1.getSemester(), g2.getSemester());
                if (cmp != 0) return cmp;
                String name1 = g1.getCourseName() != null ? g1.getCourseName() : "";
                String name2 = g2.getCourseName() != null ? g2.getCourseName() : "";
                return name1.compareToIgnoreCase(name2);
            });
        }

        return ResponseEntity.ok(list);
    }

    @GetMapping("/grades/semesters")
    public ResponseEntity<List<String>> getGradeSemesters(
            @RequestParam(required = false) String studentId) {
        String resolvedId = resolveStudentId(studentId);
        List<String> semesters = new ArrayList<>(gradeRepository.findDistinctSemestersByStudentId(resolvedId));
        if (semesters.isEmpty()) {
            String loginUser = SecurityUtils.getCurrentUsername().orElse("");
            if (!loginUser.isBlank() && !loginUser.equalsIgnoreCase(resolvedId)) {
                semesters = new ArrayList<>(gradeRepository.findDistinctSemestersByStudentId(loginUser));
            }
        }
        semesters.sort(HcmuteGradingEngine::compareSemestersDesc);
        return ResponseEntity.ok(semesters);
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<CourseSchedule>> getSchedules(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String semester) {
        String resolvedId = resolveStudentId(studentId);
        String normSem = normalizeSemester(semester);

        List<CourseSchedule> list = (normSem != null && !normSem.isBlank())
                ? scheduleRepository.findByStudentIdAndSemesterOrderByDayOfWeekAsc(resolvedId, normSem)
                : scheduleRepository.findByStudentIdOrderByDayOfWeekAsc(resolvedId);

        if (list.isEmpty() && semester != null && !semester.equalsIgnoreCase(normSem)) {
            list = scheduleRepository.findByStudentIdAndSemesterOrderByDayOfWeekAsc(resolvedId, semester);
        }

        // Nếu DB chưa có nhưng sinh viên đã liên kết Portal trong Profile -> Tự động kéo dữ liệu thật từ Portal!
        if (list.isEmpty()) {
            try {
                Long userId = SecurityUtils.getCurrentUserId().orElse(null);
                if (userId != null) {
                    String portalUser = profileService.getProfile(userId).getPortalUsername();
                    String portalPass = profileService.decodePortalPassword(userId);
                    if (portalUser != null && !portalUser.isBlank() && portalPass != null && !portalPass.isBlank()) {
                        log.info("Phát hiện tài khoản Portal đã liên kết, tự động đồng bộ thời khóa biểu thật cho {}", portalUser);
                        HcmuteSyncResponse res = crawlerService.syncStudentData(HcmuteSyncRequest.builder()
                                .studentId(portalUser)
                                .password(portalPass)
                                .build());
                        if (res.isSuccess()) {
                            list = (normSem != null && !normSem.isBlank())
                                    ? scheduleRepository.findByStudentIdAndSemesterOrderByDayOfWeekAsc(portalUser, normSem)
                                    : scheduleRepository.findByStudentIdOrderByDayOfWeekAsc(portalUser);
                            if (list.isEmpty() && semester != null && !semester.equalsIgnoreCase(normSem)) {
                                list = scheduleRepository.findByStudentIdAndSemesterOrderByDayOfWeekAsc(portalUser, semester);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Tự động đồng bộ Portal khi lấy lịch học thất bại: {}", ex.getMessage());
            }
        }

        // Fallback kiểm tra theo login username nếu studentId lưu khác
        if (list.isEmpty()) {
            String loginUser = SecurityUtils.getCurrentUsername().orElse("");
            if (!loginUser.isBlank() && !loginUser.equalsIgnoreCase(resolvedId)) {
                list = (normSem != null && !normSem.isBlank())
                        ? scheduleRepository.findByStudentIdAndSemesterOrderByDayOfWeekAsc(loginUser, normSem)
                        : scheduleRepository.findByStudentIdOrderByDayOfWeekAsc(loginUser);
                if (list.isEmpty() && semester != null && !semester.equalsIgnoreCase(normSem)) {
                    list = scheduleRepository.findByStudentIdAndSemesterOrderByDayOfWeekAsc(loginUser, semester);
                }
            }
        }

        // Tự động chuẩn hóa thời gian các môn học đã lưu trong DB nếu trước đó bị gán nhầm 07:30
        if (!list.isEmpty()) {
            boolean updated = false;
            for (CourseSchedule cs : list) {
                if (cs.getShift() != null && !cs.getShift().isBlank()) {
                    String[] times = HcmuteCrawlerService.calculateTimesFromShift(cs.getShift());
                    if ("07:30".equals(cs.getStartTime()) && !"07:30".equals(times[0])) {
                        cs.setStartTime(times[0]);
                        cs.setEndTime(times[1]);
                        updated = true;
                    }
                }
            }
            if (updated) {
                scheduleRepository.saveAll(list);
            }
        }

        return ResponseEntity.ok(list);
    }

    private String normalizeSemester(String semester) {
        if (semester == null || semester.isBlank()) return semester;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(?:HK0?(\\d)|Học kỳ (\\d)|HK(\\d))\\s*\\((.+)\\)$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(semester.trim());
        if (m.find()) {
            String termNum = m.group(1) != null ? m.group(1) : (m.group(2) != null ? m.group(2) : m.group(3));
            String year = m.group(4);
            if ("3".equals(termNum)) {
                return "Học kỳ 3 (Hè) (" + year + ")";
            }
            return "Học kỳ " + termNum + " (" + year + ")";
        }
        return semester.trim();
    }

    @GetMapping("/curriculum")
    public ResponseEntity<List<CurriculumSubject>> getCurriculum(
            @RequestParam(required = false) String studentId) {
        String resolvedId = resolveStudentId(studentId);
        return ResponseEntity.ok(curriculumRepository.findByStudentIdOrderBySemesterRecommendedAsc(resolvedId));
    }

    @GetMapping("/curriculum/summary")
    public ResponseEntity<CurriculumSummaryDto> getCurriculumSummary(
            @RequestParam(required = false) String studentId) {
        String resolvedId = resolveStudentId(studentId);
        return ResponseEntity.ok(crawlerService.getCurriculumSummary(resolvedId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SyncHistory>> getHistory(
            @RequestParam(required = false) String studentId) {
        String resolvedId = resolveStudentId(studentId);
        return ResponseEntity.ok(syncHistoryRepository.findByStudentIdOrderBySyncedAtDesc(resolvedId));
    }

    private String resolveStudentId(String studentId) {
        if (studentId != null && !studentId.isBlank()) {
            return studentId.trim();
        }
        // Ưu tiên MSSV/Portal Username đã liên kết trong Profile
        try {
            Long userId = SecurityUtils.getCurrentUserId().orElse(null);
            if (userId != null) {
                String portalUsername = profileService.getProfile(userId).getPortalUsername();
                if (portalUsername != null && !portalUsername.isBlank()) {
                    return portalUsername.trim();
                }
            }
        } catch (Exception ignored) {}

        return SecurityUtils.getCurrentUsername().orElse("21110001");
    }
}

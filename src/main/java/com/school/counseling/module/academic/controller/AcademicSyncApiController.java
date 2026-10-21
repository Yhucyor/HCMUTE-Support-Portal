package com.school.counseling.module.academic.controller;

import com.school.counseling.common.util.SecurityUtils;
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
import com.school.counseling.module.academic.repository.SyncHistoryRepository;
import com.school.counseling.module.academic.service.HcmuteCrawlerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.school.counseling.module.auth.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/academic")
public class AcademicSyncApiController {

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

        return ResponseEntity.ok(list);
    }

    @GetMapping("/grades/semesters")
    public ResponseEntity<List<String>> getGradeSemesters(
            @RequestParam(required = false) String studentId) {
        String resolvedId = resolveStudentId(studentId);
        List<String> semesters = gradeRepository.findDistinctSemestersByStudentId(resolvedId);
        if (semesters.isEmpty()) {
            String loginUser = SecurityUtils.getCurrentUsername().orElse("");
            if (!loginUser.isBlank() && !loginUser.equalsIgnoreCase(resolvedId)) {
                semesters = gradeRepository.findDistinctSemestersByStudentId(loginUser);
            }
        }
        return ResponseEntity.ok(semesters);
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<CourseSchedule>> getSchedules(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String semester) {
        String resolvedId = resolveStudentId(studentId);

        List<CourseSchedule> list = (semester != null && !semester.isBlank())
                ? scheduleRepository.findByStudentIdAndSemesterOrderByDayOfWeekAsc(resolvedId, semester)
                : scheduleRepository.findByStudentIdOrderByDayOfWeekAsc(resolvedId);

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
                            list = (semester != null && !semester.isBlank())
                                    ? scheduleRepository.findByStudentIdAndSemesterOrderByDayOfWeekAsc(portalUser, semester)
                                    : scheduleRepository.findByStudentIdOrderByDayOfWeekAsc(portalUser);
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
                list = (semester != null && !semester.isBlank())
                        ? scheduleRepository.findByStudentIdAndSemesterOrderByDayOfWeekAsc(loginUser, semester)
                        : scheduleRepository.findByStudentIdOrderByDayOfWeekAsc(loginUser);
            }
        }

        return ResponseEntity.ok(list);
    }

    @GetMapping("/curriculum")
    public ResponseEntity<List<CurriculumSubject>> getCurriculum(
            @RequestParam(required = false) String studentId) {
        String resolvedId = resolveStudentId(studentId);
        return ResponseEntity.ok(curriculumRepository.findByStudentIdOrderBySemesterRecommendedAsc(resolvedId));
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

package com.school.counseling.module.academic.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.academic.dto.AcademicSummaryResponse;
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
import com.school.counseling.module.academic.service.HcmuteCrawlerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic")
@RequiredArgsConstructor
public class AcademicSyncApiController {

    private final HcmuteCrawlerService crawlerService;
    private final StudentGradeRepository gradeRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final CurriculumSubjectRepository curriculumRepository;
    private final SyncHistoryRepository syncHistoryRepository;

    @PostMapping("/sync")
    public ResponseEntity<HcmuteSyncResponse> syncAcademicData(@Valid @RequestBody HcmuteSyncRequest request) {
        HcmuteSyncResponse response = crawlerService.syncStudentData(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<AcademicSummaryResponse> getAcademicSummary(
            @RequestParam(required = false) String studentId) {
        String resolvedId = resolveStudentId(studentId);
        return ResponseEntity.ok(crawlerService.getAcademicSummary(resolvedId));
    }

    @GetMapping("/grades")
    public ResponseEntity<List<StudentGrade>> getGrades(
            @RequestParam(required = false) String studentId) {
        String resolvedId = resolveStudentId(studentId);
        return ResponseEntity.ok(gradeRepository.findByStudentIdOrderBySemesterDesc(resolvedId));
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<CourseSchedule>> getSchedules(
            @RequestParam(required = false) String studentId) {
        String resolvedId = resolveStudentId(studentId);
        return ResponseEntity.ok(scheduleRepository.findByStudentIdOrderByDayOfWeekAsc(resolvedId));
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
        return SecurityUtils.getCurrentUsername().orElse("21110001");
    }
}

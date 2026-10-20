package com.school.counseling.module.academic.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.academic.dto.AcademicSummaryResponse;
import com.school.counseling.module.academic.service.HcmuteCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/academic")
public class AcademicWebController {

    private final HcmuteCrawlerService crawlerService;

    public AcademicWebController(HcmuteCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping("/sync")
    public String syncPage(@org.springframework.web.bind.annotation.RequestParam(required = false) String studentId,
                          @org.springframework.web.bind.annotation.RequestParam(required = false) String tab,
                          Model model) {
        String effectiveStudentId = (studentId != null && !studentId.isBlank())
                ? studentId.trim()
                : SecurityUtils.getCurrentUsername().orElse("");

        // Xác định active tab từ query param, default là "academic"
        String activeTab = (tab != null && !tab.isBlank()) ? tab : "academic";
        
        model.addAttribute("currentStudentId", effectiveStudentId);
        model.addAttribute("activeTab", activeTab);

        if (!effectiveStudentId.isBlank()) {
            AcademicSummaryResponse summary = crawlerService.getAcademicSummary(effectiveStudentId);
            model.addAttribute("summary", summary);
            crawlerService.getStudentProfile(effectiveStudentId).ifPresent(profile ->
                    model.addAttribute("studentProfile", profile));
        }

        return "academic-sync";
    }

    /**
     * Route cho Bảng điểm - Requirement 5
     * Navigate với transcript tab active
     */
    @GetMapping("/grades")
    public String gradesPage(@org.springframework.web.bind.annotation.RequestParam(required = false) String studentId, Model model) {
        String effectiveStudentId = (studentId != null && !studentId.isBlank())
                ? studentId.trim()
                : SecurityUtils.getCurrentUsername().orElse("");

        model.addAttribute("currentStudentId", effectiveStudentId);
        model.addAttribute("activeTab", "transcript");

        if (!effectiveStudentId.isBlank()) {
            AcademicSummaryResponse summary = crawlerService.getAcademicSummary(effectiveStudentId);
            model.addAttribute("summary", summary);
            crawlerService.getStudentProfile(effectiveStudentId).ifPresent(profile ->
                    model.addAttribute("studentProfile", profile));
        }

        return "academic-sync";
    }

    /**
     * Route cho Thời khóa biểu - Requirement 6
     * Navigate với schedule tab active
     */
    @GetMapping("/timetable")
    public String timetablePage(@org.springframework.web.bind.annotation.RequestParam(required = false) String studentId, Model model) {
        String effectiveStudentId = (studentId != null && !studentId.isBlank())
                ? studentId.trim()
                : SecurityUtils.getCurrentUsername().orElse("");

        model.addAttribute("currentStudentId", effectiveStudentId);
        model.addAttribute("activeTab", "schedule");

        if (!effectiveStudentId.isBlank()) {
            AcademicSummaryResponse summary = crawlerService.getAcademicSummary(effectiveStudentId);
            model.addAttribute("summary", summary);
            crawlerService.getStudentProfile(effectiveStudentId).ifPresent(profile ->
                    model.addAttribute("studentProfile", profile));
        }

        return "academic-sync";
    }

    /**
     * Route cho Chương trình đào tạo - Requirement 7
     * Navigate với curriculum tab active
     */
    @GetMapping("/curriculum")
    public String curriculumPage(@org.springframework.web.bind.annotation.RequestParam(required = false) String studentId, Model model) {
        String effectiveStudentId = (studentId != null && !studentId.isBlank())
                ? studentId.trim()
                : SecurityUtils.getCurrentUsername().orElse("");

        model.addAttribute("currentStudentId", effectiveStudentId);
        model.addAttribute("activeTab", "curriculum");

        if (!effectiveStudentId.isBlank()) {
            AcademicSummaryResponse summary = crawlerService.getAcademicSummary(effectiveStudentId);
            model.addAttribute("summary", summary);
            crawlerService.getStudentProfile(effectiveStudentId).ifPresent(profile ->
                    model.addAttribute("studentProfile", profile));
        }

        return "academic-sync";
    }

    @GetMapping({"", "/", "/info"})
    public String redirectToSync() {
        return "redirect:/academic/sync";
    }
}

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
    public String syncPage(@org.springframework.web.bind.annotation.RequestParam(required = false) String studentId, Model model) {
        String effectiveStudentId = (studentId != null && !studentId.isBlank())
                ? studentId.trim()
                : SecurityUtils.getCurrentUsername().orElse("");

        model.addAttribute("currentStudentId", effectiveStudentId);
        model.addAttribute("activeTab", "academic");

        if (!effectiveStudentId.isBlank()) {
            AcademicSummaryResponse summary = crawlerService.getAcademicSummary(effectiveStudentId);
            model.addAttribute("summary", summary);
            crawlerService.getStudentProfile(effectiveStudentId).ifPresent(profile ->
                    model.addAttribute("studentProfile", profile));
        }

        return "academic-sync";
    }

    @GetMapping({"", "/", "/grades", "/timetable", "/curriculum", "/info"})
    public String redirectToSync() {
        return "redirect:/academic/sync";
    }
}

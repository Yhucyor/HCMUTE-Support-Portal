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
@RequiredArgsConstructor
public class AcademicWebController {

    private final HcmuteCrawlerService crawlerService;

    @GetMapping("/sync")
    public String syncPage(Model model) {
        String currentStudentId = SecurityUtils.getCurrentUsername().orElse("");
        model.addAttribute("currentStudentId", currentStudentId);
        model.addAttribute("activeTab", "academic");

        if (!currentStudentId.isBlank()) {
            AcademicSummaryResponse summary = crawlerService.getAcademicSummary(currentStudentId);
            model.addAttribute("summary", summary);
        }

        return "academic-sync";
    }

    @GetMapping({"", "/", "/grades", "/timetable", "/curriculum"})
    public String redirectToSync() {
        return "redirect:/academic/sync";
    }
}

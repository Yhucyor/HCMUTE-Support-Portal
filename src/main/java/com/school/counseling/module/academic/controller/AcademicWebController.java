package com.school.counseling.module.academic.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.academic.dto.AcademicSummaryResponse;
import com.school.counseling.module.academic.service.HcmuteCrawlerService;
import com.school.counseling.module.auth.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/academic")
public class AcademicWebController {

    private final HcmuteCrawlerService crawlerService;
    private final ProfileService profileService;

    public AcademicWebController(HcmuteCrawlerService crawlerService,
                                 ProfileService profileService) {
        this.crawlerService = crawlerService;
        this.profileService = profileService;
    }

    @GetMapping("/sync")
    public String syncPage(@RequestParam(required = false) String studentId,
                           @RequestParam(required = false) String tab,
                           Model model) {
        String effectiveStudentId = (studentId != null && !studentId.isBlank())
                ? studentId.trim()
                : SecurityUtils.getCurrentUsername().orElse("");

        String activeTab = (tab != null && !tab.isBlank()) ? tab : "academic";

        model.addAttribute("currentStudentId", effectiveStudentId);
        model.addAttribute("activeTab", activeTab);

        // Lấy portal credentials đã lưu để auto-sync
        populatePortalCredentials(model);

        if (!effectiveStudentId.isBlank()) {
            AcademicSummaryResponse summary = crawlerService.getAcademicSummary(effectiveStudentId);
            model.addAttribute("summary", summary);
            crawlerService.getStudentProfile(effectiveStudentId).ifPresent(profile ->
                    model.addAttribute("studentProfile", profile));
        }

        return "academic-sync";
    }

    @GetMapping("/grades")
    public String gradesPage(@RequestParam(required = false) String studentId, Model model) {
        String effectiveStudentId = resolve(studentId);
        model.addAttribute("currentStudentId", effectiveStudentId);
        model.addAttribute("activeTab", "transcript");
        populatePortalCredentials(model);
        if (!effectiveStudentId.isBlank()) {
            model.addAttribute("summary", crawlerService.getAcademicSummary(effectiveStudentId));
            crawlerService.getStudentProfile(effectiveStudentId).ifPresent(p -> model.addAttribute("studentProfile", p));
        }
        return "academic-sync";
    }

    @GetMapping("/timetable")
    public String timetablePage(@RequestParam(required = false) String studentId, Model model) {
        populatePortalCredentials(model);
        model.addAttribute("currentStudentId", resolve(studentId));
        return "timetable";
    }

    @GetMapping("/curriculum")
    public String curriculumPage(@RequestParam(required = false) String studentId, Model model) {
        String effectiveStudentId = resolve(studentId);
        model.addAttribute("currentStudentId", effectiveStudentId);
        model.addAttribute("activeTab", "curriculum");
        populatePortalCredentials(model);
        if (!effectiveStudentId.isBlank()) {
            model.addAttribute("summary", crawlerService.getAcademicSummary(effectiveStudentId));
            crawlerService.getStudentProfile(effectiveStudentId).ifPresent(p -> model.addAttribute("studentProfile", p));
        }
        return "academic-sync";
    }

    @GetMapping({"", "/", "/info"})
    public String redirectToSync() {
        return "redirect:/academic/sync";
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String resolve(String studentId) {
        return (studentId != null && !studentId.isBlank())
                ? studentId.trim()
                : SecurityUtils.getCurrentUsername().orElse("");
    }

    /**
     * Đưa portal credentials đã lưu vào Model.
     * Template dùng để tự động điền form và trigger auto-sync.
     */
    private void populatePortalCredentials(Model model) {
        try {
            SecurityUtils.getCurrentUserId().ifPresent(userId -> {
                String portalUsername = profileService.getProfile(userId).getPortalUsername();
                String portalPassword = profileService.decodePortalPassword(userId);

                boolean hasCredentials = portalUsername != null && !portalUsername.isBlank()
                        && portalPassword != null && !portalPassword.isBlank();

                model.addAttribute("portalLinked", hasCredentials);
                if (hasCredentials) {
                    model.addAttribute("savedPortalUsername", portalUsername);
                    // Truyền password qua model chỉ trong scope request server-side.
                    // Template dùng th:data-* ẩn để JS tự fill — KHÔNG render ra HTML visible.
                    model.addAttribute("savedPortalPassword", portalPassword);
                }
            });
        } catch (Exception e) {
            log.debug("Không thể lấy portal credentials: {}", e.getMessage());
            model.addAttribute("portalLinked", false);
        }
    }
}

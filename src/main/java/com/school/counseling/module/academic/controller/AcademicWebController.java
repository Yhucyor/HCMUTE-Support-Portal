package com.school.counseling.module.academic.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.academic.dto.AcademicSummaryResponse;
import com.school.counseling.module.academic.dto.CurriculumSummaryDto;
import com.school.counseling.module.academic.service.HcmuteCrawlerService;
import com.school.counseling.module.auth.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/academic")
public class AcademicWebController {

    private static final Logger log = LoggerFactory.getLogger(AcademicWebController.class);

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
        String effectiveStudentId = resolve(studentId);

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

        // Nạp nhanh thông báo từ cache RAM hoặc demo (không gọi HTTP bên ngoài đồng bộ gây treo luồng)
        String portalUser = (String) model.getAttribute("savedPortalUsername");
        String portalPass = SecurityUtils.getCurrentUserId()
                .map(profileService::decodePortalPassword)
                .orElse(null);
        model.addAttribute("announcements", crawlerService.getQuickAnnouncements(
                portalUser != null ? portalUser : effectiveStudentId, portalPass));

        return "academic-sync";
    }

    @GetMapping("/grades")
    public String gradesPage(@RequestParam(required = false) String studentId, Model model) {
        String effectiveStudentId = resolve(studentId);
        model.addAttribute("currentStudentId", effectiveStudentId);
        model.addAttribute("activeTab", "transcript");
        populatePortalCredentials(model);
        SecurityUtils.getCurrentUser().ifPresent(user -> model.addAttribute("currentUser", user));
        if (!effectiveStudentId.isBlank()) {
            model.addAttribute("summary", crawlerService.getAcademicSummary(effectiveStudentId));
            crawlerService.getStudentProfile(effectiveStudentId).ifPresent(p -> model.addAttribute("studentProfile", p));
        }
        return "grades";
    }

    @GetMapping("/timetable")
    public String timetablePage(@RequestParam(required = false) String studentId, Model model) {
        populatePortalCredentials(model);
        SecurityUtils.getCurrentUser().ifPresent(user -> model.addAttribute("currentUser", user));
        model.addAttribute("currentStudentId", resolve(studentId));
        return "timetable";
    }

    @GetMapping("/curriculum")
    public String curriculumPage(@RequestParam(required = false) String studentId, Model model) {
        String effectiveStudentId = resolve(studentId);
        model.addAttribute("currentStudentId", effectiveStudentId);
        model.addAttribute("activeTab", "curriculum");
        populatePortalCredentials(model);
        SecurityUtils.getCurrentUser().ifPresent(user -> model.addAttribute("currentUser", user));

        CurriculumSummaryDto curriculum = crawlerService.getCurriculumSummary(effectiveStudentId);
        model.addAttribute("curriculum", curriculum);

        if (!effectiveStudentId.isBlank()) {
            model.addAttribute("summary", crawlerService.getAcademicSummary(effectiveStudentId));
            crawlerService.getStudentProfile(effectiveStudentId).ifPresent(p -> model.addAttribute("studentProfile", p));
        }
        return "curriculum";
    }

    @GetMapping({"", "/", "/info"})
    public String redirectToSync() {
        return "redirect:/academic/sync";
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String resolve(String studentId) {
        String currentUserStudentId = null;
        try {
            Long userId = SecurityUtils.getCurrentUserId().orElse(null);
            if (userId != null) {
                String portalUsername = profileService.getProfile(userId).getPortalUsername();
                if (portalUsername != null && !portalUsername.isBlank()) {
                    currentUserStudentId = portalUsername.trim();
                }
            }
        } catch (Exception ignored) {}

        if (currentUserStudentId == null || currentUserStudentId.isBlank()) {
            currentUserStudentId = SecurityUtils.getCurrentUsername().orElse("");
        }

        if (studentId != null && !studentId.isBlank()) {
            String trimmed = studentId.trim();
            boolean isStaffOrAdmin = SecurityUtils.hasAnyRole("STAFF", "DEPARTMENT_HEAD", "ADMIN", "SUPER_ADMIN");
            if (isStaffOrAdmin || (currentUserStudentId != null && currentUserStudentId.equalsIgnoreCase(trimmed))) {
                return trimmed;
            }
            log.warn("⚠️ [Security Anti-IDOR] Web: Sinh viên '{}' cố gắng xem học vụ của '{}' - Đã chặn và điều hướng về trang cá nhân.",
                    currentUserStudentId, trimmed);
            return currentUserStudentId;
        }

        return currentUserStudentId;
    }

    /**
     * Đưa portal credentials đã lưu vào Model.
     * Template dùng để tự động điền form và trigger auto-sync.
     */
    private void populatePortalCredentials(Model model) {
        model.addAttribute("portalLinked", false);
        try {
            SecurityUtils.getCurrentUserId().ifPresent(userId -> {
                String portalUsername = profileService.getProfile(userId).getPortalUsername();
                String portalPassword = profileService.decodePortalPassword(userId);

                boolean hasCredentials = portalUsername != null && !portalUsername.isBlank()
                        && portalPassword != null && !portalPassword.isBlank();

                model.addAttribute("portalLinked", hasCredentials);
                if (hasCredentials) {
                    model.addAttribute("savedPortalUsername", portalUsername);
                    // Không truyền mật khẩu plain-text vào Model để chống lộ lọt phía Client DOM.
                    // Sử dụng token che giấu __SAVED__; API Backend sẽ tự động nạp mật khẩu an toàn.
                    model.addAttribute("savedPortalPassword", "__SAVED__");
                }
            });
        } catch (Exception e) {
            log.debug("Không thể lấy portal credentials: {}", e.getMessage());
            model.addAttribute("portalLinked", false);
        }
    }
}

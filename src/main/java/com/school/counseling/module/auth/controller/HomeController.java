package com.school.counseling.module.auth.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final DepartmentRepository departmentRepository;
    private final com.school.counseling.module.academic.service.HcmuteCrawlerService crawlerService;
    private final com.school.counseling.module.ticket.service.TicketService ticketService;

    public HomeController(DepartmentRepository departmentRepository,
                          com.school.counseling.module.academic.service.HcmuteCrawlerService crawlerService,
                          com.school.counseling.module.ticket.service.TicketService ticketService) {
        this.departmentRepository = departmentRepository;
        this.crawlerService = crawlerService;
        this.ticketService = ticketService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        if (SecurityUtils.hasRole("ROLE_ADMIN")) {
            return "redirect:/admin/dashboard";
        } else if (SecurityUtils.hasRole("ROLE_STAFF")) {
            return "redirect:/staff/tickets";
        }

        UserPrincipal currentUser = SecurityUtils.getCurrentUser().orElse(null);
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("currentUser", currentUser);

        try {
            crawlerService.getStudentProfile(currentUser.getUsername())
                    .ifPresent(p -> model.addAttribute("studentProfile", p));
            model.addAttribute("academicSummary", crawlerService.getAcademicSummary(currentUser.getUsername()));
        } catch (Exception ignored) {
        }

        try {
            model.addAttribute("myTickets", ticketService.getMyTicketSummaries(currentUser.getId()));
        } catch (Exception ignored) {
        }

        return "dashboard";
    }

    /**
     * Student dashboard route: /student/dashboard
     * Requirement 4: Menu Item "Tổng quan" sẽ navigate đến route này
     */
    @GetMapping("/student/dashboard")
    public String studentDashboard(Model model) {
        // Chỉ cho phép ROLE_STUDENT truy cập
        if (!SecurityUtils.hasRole("ROLE_STUDENT")) {
            return "redirect:/dashboard";
        }
        
        // Redirect đến /dashboard chung (cùng template)
        return "redirect:/dashboard";
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("departments", departmentRepository.findByIsActiveTrue());
        SecurityUtils.getCurrentUser().ifPresent(user -> model.addAttribute("currentUser", user));
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        return "about";
    }

    @GetMapping("/admission")
    public String admission(Model model) {
        return "admission";
    }

    @GetMapping("/research")
    public String research(Model model) {
        return "research";
    }

    @GetMapping("/explore")
    public String explore(Model model) {
        model.addAttribute("departments", departmentRepository.findByIsActiveTrue());
        return "explore";
    }

    @GetMapping("/ai-chat")
    public String aiChat(jakarta.servlet.http.HttpServletRequest request, Model model) {
        request.getSession(true);
        UserPrincipal currentUser = SecurityUtils.getCurrentUser().orElse(null);
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("currentUserFullName", currentUser.getFullName());
            model.addAttribute("currentUserAvatar", currentUser.getAvatarUrl());
            String role = (currentUser.getRole() != null) ? currentUser.getRole().replace("ROLE_", "") : "STUDENT";
            model.addAttribute("currentUserRole", role);
            model.addAttribute("currentUserDept", currentUser.getDepartmentName() != null ? currentUser.getDepartmentName() : "Toàn trường");
        }
        model.addAttribute("departments", departmentRepository.findByIsActiveTrue());
        return "ai-chat";
    }
}


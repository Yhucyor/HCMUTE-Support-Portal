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

    @GetMapping({"/dashboard", "/student/dashboard"})
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

        return "redirect:/academic/sync";
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

    @GetMapping({"/grades", "/academic-grades"})
    public String redirectGrades() {
        return "redirect:/academic/grades";
    }

    @GetMapping({"/timetable", "/academic-timetable"})
    public String redirectTimetable() {
        return "redirect:/academic/timetable";
    }

    @GetMapping({"/curriculum", "/academic-curriculum", "/chuong-trinh-dao-tao"})
    public String redirectCurriculum() {
        return "redirect:/academic/curriculum";
    }
}


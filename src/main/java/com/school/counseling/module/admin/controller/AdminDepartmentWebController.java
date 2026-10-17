package com.school.counseling.module.admin.controller;

import com.school.counseling.module.admin.dto.DepartmentAdminDto;
import com.school.counseling.module.admin.service.AdminDepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/departments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDepartmentWebController {

    private final AdminDepartmentService adminDepartmentService;

    @GetMapping
    public String listDepartments(Model model) {
        List<DepartmentAdminDto> departments = adminDepartmentService.getAllDepartmentsWithStats();
        model.addAttribute("departments", departments);
        return "admin/departments";
    }
}

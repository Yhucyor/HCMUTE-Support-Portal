package com.school.counseling.module.admin.controller;

import com.school.counseling.module.admin.dto.UserManagementDto;
import com.school.counseling.module.admin.service.AdminUserService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserWebController {

    private final AdminUserService adminUserService;

    @GetMapping
    public String listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "roleId", required = false) Long roleId,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            Model model) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<UserManagementDto> usersPage = adminUserService.getUsers(keyword, roleId, departmentId, status, pageable);
        List<Role> roles = adminUserService.getAllRoles();
        List<Department> departments = adminUserService.getAllDepartments();

        model.addAttribute("usersPage", usersPage);
        model.addAttribute("roles", roles);
        model.addAttribute("departments", departments);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRoleId", roleId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedStatus", status);

        return "admin/users";
    }
}

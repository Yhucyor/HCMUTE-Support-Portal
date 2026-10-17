package com.school.counseling.module.admin.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.module.admin.dto.AdminSlaDashboardDto;
import com.school.counseling.module.admin.service.AdminDashboardService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardWebController {

    private final AdminDashboardService adminDashboardService;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public String adminIndex() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String adminSlaDashboard(
            @RequestParam(value = "timeRange", required = false, defaultValue = "ALL") String timeRange,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            Model model) {

        AdminSlaDashboardDto stats = adminDashboardService.getSlaDashboardStats(timeRange, departmentId);
        List<Department> departments = departmentRepository.findAll();

        model.addAttribute("stats", stats);
        model.addAttribute("departments", departments);
        model.addAttribute("selectedTimeRange", timeRange);
        model.addAttribute("selectedDepartmentId", departmentId);

        return "admin/dashboard";
    }

    @ResponseBody
    @GetMapping("/api/stats")
    public ResponseEntity<ApiResponse<AdminSlaDashboardDto>> getDashboardStatsJson(
            @RequestParam(value = "timeRange", required = false, defaultValue = "ALL") String timeRange,
            @RequestParam(value = "departmentId", required = false) Long departmentId) {

        AdminSlaDashboardDto stats = adminDashboardService.getSlaDashboardStats(timeRange, departmentId);
        return ResponseEntity.ok(ApiResponse.success(stats, "Tải dữ liệu thống kê SLA thành công"));
    }
}

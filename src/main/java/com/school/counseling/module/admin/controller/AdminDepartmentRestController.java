package com.school.counseling.module.admin.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.module.admin.dto.DepartmentAdminDto;
import com.school.counseling.module.admin.dto.SaveDepartmentDto;
import com.school.counseling.module.admin.service.AdminDepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/departments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDepartmentRestController {

    private final AdminDepartmentService adminDepartmentService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentAdminDto>> getDepartmentById(@PathVariable Long id) {
        DepartmentAdminDto dept = adminDepartmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.success(dept, "Tải thông tin Đơn vị thành công"));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<DepartmentAdminDto>> saveDepartment(
            @Valid @RequestBody SaveDepartmentDto dto) {
        DepartmentAdminDto saved = adminDepartmentService.saveDepartment(dto);
        String msg = (dto.getId() != null)
                ? "Cập nhật thông tin Đơn vị [" + saved.getName() + "] thành công!"
                : "Thêm mới Đơn vị [" + saved.getName() + "] thành công!";
        return ResponseEntity.ok(ApiResponse.success(saved, msg));
    }

    @PostMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<DepartmentAdminDto>> toggleActive(@PathVariable Long id) {
        DepartmentAdminDto updated = adminDepartmentService.toggleDepartmentStatus(id);
        String msg = Boolean.TRUE.equals(updated.getIsActive())
                ? "Đã kích hoạt hoạt động cho Đơn vị [" + updated.getName() + "]"
                : "Đã tạm dừng hoạt động Đơn vị [" + updated.getName() + "]";
        return ResponseEntity.ok(ApiResponse.success(updated, msg));
    }
}

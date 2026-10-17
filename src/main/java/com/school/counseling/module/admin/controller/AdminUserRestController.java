package com.school.counseling.module.admin.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.module.admin.dto.UpdateUserAdminDto;
import com.school.counseling.module.admin.dto.UserManagementDto;
import com.school.counseling.module.admin.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserRestController {

    private final AdminUserService adminUserService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserManagementDto>> getUserById(@PathVariable Long id) {
        UserManagementDto user = adminUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "Tải thông tin người dùng thành công"));
    }

    @PostMapping("/{id}/toggle-lock")
    public ResponseEntity<ApiResponse<UserManagementDto>> toggleUserLock(@PathVariable Long id) {
        try {
            UserManagementDto updated = adminUserService.toggleUserLock(id);
            String message = "LOCKED".equalsIgnoreCase(updated.getStatus())
                    ? "Đã khóa tài khoản [" + updated.getUsername() + "] thành công!"
                    : "Đã mở khóa kích hoạt tài khoản [" + updated.getUsername() + "] thành công!";
            return ResponseEntity.ok(ApiResponse.success(updated, message));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/{id}/update")
    public ResponseEntity<ApiResponse<UserManagementDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserAdminDto dto) {
        UserManagementDto updated = adminUserService.updateUser(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật thông tin người dùng [" + updated.getUsername() + "] thành công!"));
    }
}

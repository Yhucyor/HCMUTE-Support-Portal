package com.school.counseling.module.auth.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.academic.dto.HcmuteSyncRequest;
import com.school.counseling.module.academic.dto.HcmuteSyncResponse;
import com.school.counseling.module.academic.service.HcmuteCrawlerService;
import com.school.counseling.module.auth.dto.ChangePasswordRequest;
import com.school.counseling.module.auth.dto.UpdateProfileRequest;
import com.school.counseling.module.auth.dto.UserProfileDto;
import com.school.counseling.module.auth.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProfileRestController {

    private final ProfileService profileService;
    private final HcmuteCrawlerService crawlerService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("Người dùng chưa đăng nhập"));
        UserProfileDto profile = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("Người dùng chưa đăng nhập"));
        UserProfileDto updated = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật thông tin thành công"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("Người dùng chưa đăng nhập"));
        profileService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("Người dùng chưa đăng nhập"));
        String avatarUrl = profileService.updateAvatar(userId, file);
        return ResponseEntity.ok(ApiResponse.success(avatarUrl, "Cập nhật ảnh đại diện thành công"));
    }

    /**
     * Kiểm tra kết nối Portal: thực sự gọi API sync để xác minh credentials hợp lệ.
     * Body: { "portalUsername": "...", "portalPassword": "..." }
     */
    @PostMapping("/portal/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPortalCredentials(
            @RequestBody Map<String, String> body) {
        String username = body.getOrDefault("portalUsername", "").trim();
        String password = body.getOrDefault("portalPassword", "").trim();

        if (username.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Vui lòng nhập đầy đủ tài khoản và mật khẩu Portal"));
        }

        try {
            HcmuteSyncRequest syncRequest = HcmuteSyncRequest.builder()
                    .studentId(username)
                    .password(password)
                    .build();
            HcmuteSyncResponse result = crawlerService.syncStudentData(syncRequest);

            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(
                        Map.of("verified", true, "syncMode", result.getSyncMode() != null ? result.getSyncMode() : ""),
                        "Kết nối Portal thành công! Tài khoản hợp lệ."));
            } else {
                return ResponseEntity.ok(ApiResponse.error(
                        result.getMessage() != null ? result.getMessage() : "Xác thực thất bại, kiểm tra lại tài khoản hoặc mật khẩu."));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Không thể kết nối đến Portal HCMUTE: " + e.getMessage()));
        }
    }
}

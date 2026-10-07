package com.school.counseling.module.auth.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.dto.AuthResponse;
import com.school.counseling.module.auth.dto.LoginRequest;
import com.school.counseling.module.auth.dto.RegisterRequest;
import com.school.counseling.module.auth.dto.UserProfileDto;
import com.school.counseling.module.auth.service.AuthService;
import com.school.counseling.module.auth.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthService authService;
    private final ProfileService profileService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileDto>> register(@Valid @RequestBody RegisterRequest request) {
        UserProfileDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Đăng ký thành công. Mã OTP kích hoạt đã được gửi tới email."));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@RequestParam String email, @RequestParam String otpCode) {
        authService.activateAccount(email, otpCode);
        return ResponseEntity.ok(ApiResponse.success(null, "Kích hoạt tài khoản thành công"));
    }

    @PostMapping("/resend-activation-otp")
    public ResponseEntity<ApiResponse<Void>> resendActivationOtp(@RequestParam String email) {
        authService.resendActivationOtp(email);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã gửi lại mã OTP kích hoạt"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.success(null, "Mã OTP đã được gửi về email của bạn"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestParam String email,
                                                           @RequestParam String otpCode,
                                                           @RequestParam String newPassword) {
        authService.resetPassword(email, otpCode, newPassword);
        return ResponseEntity.ok(ApiResponse.success(null, "Đặt lại mật khẩu thành công"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("Người dùng chưa đăng nhập"));
        UserProfileDto profile = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}

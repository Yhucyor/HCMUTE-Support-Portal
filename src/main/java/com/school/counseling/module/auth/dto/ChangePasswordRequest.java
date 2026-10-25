package com.school.counseling.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    // Mật khẩu hiện tại (bắt buộc với tài khoản thường, không bắt buộc với tài khoản Google OAuth)
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu mới phải từ 6 ký tự trở lên")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu mới không được để trống")
    private String confirmPassword;

    @NotBlank(message = "Mã xác thực OTP không được để trống")
    @Size(min = 6, max = 6, message = "Mã OTP phải gồm 6 chữ số")
    private String otpCode;
}

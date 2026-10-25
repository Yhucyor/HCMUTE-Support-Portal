package com.school.counseling.module.auth.service;

import com.school.counseling.common.exception.AppException;
import com.school.counseling.common.storage.IStorageService;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.dto.ChangePasswordRequest;
import com.school.counseling.module.auth.dto.UpdateProfileRequest;
import com.school.counseling.module.auth.dto.UserProfileDto;
import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final IStorageService storageService;
    private final OtpService otpService;
    private final com.school.counseling.module.notification.service.EmailAsyncService emailAsyncService;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Không tìm thấy thông tin người dùng"));
        return UserProfileDto.fromEntity(user);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng"));

        // Check email uniqueness if modified
        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email đã được sử dụng bởi tài khoản khác");
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
            user.setDepartment(department);
        }

        // Lưu Portal credentials nếu được cung cấp
        if (request.getPortalUsername() != null && !request.getPortalUsername().isBlank()) {
            user.setPortalUsername(request.getPortalUsername().trim());
        }
        if (request.getPortalPassword() != null && !request.getPortalPassword().isBlank()) {
            // Mã hoá Base64 — không dùng bcrypt vì cần giải mã để gọi API Portal
            String encoded = Base64.getEncoder().encodeToString(
                    request.getPortalPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            user.setPortalPasswordEnc(encoded);
            log.info("Đã lưu Portal credentials cho user {}", userId);
        }

        User updated = userRepository.save(user);
        return UserProfileDto.fromEntity(updated);
    }

    /**
     * Giải mã mật khẩu Portal để dùng khi đồng bộ học vụ.
     */
    @Transactional(readOnly = true)
    public String decodePortalPassword(Long userId) {
        return userRepository.findById(userId)
                .map(User::getPortalPasswordEnc)
                .filter(enc -> enc != null && !enc.isBlank())
                .map(enc -> new String(Base64.getDecoder().decode(enc), java.nio.charset.StandardCharsets.UTF_8))
                .orElse(null);
    }

    @Transactional
    public String sendChangePasswordOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng"));

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new AppException("Tài khoản chưa có thông tin email để nhận mã OTP!");
        }

        String otp = otpService.generateOtp(user.getEmail(), "CHANGE_PASSWORD");
        log.info("Change password OTP generated for user {}: {} (Destination email: {})", user.getUsername(), otp, user.getEmail());
        emailAsyncService.sendOtpEmail(user.getEmail(), user.getFullName(), otp, "xác thực đổi mật khẩu tài khoản");
        return user.getEmail();
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Mật khẩu xác nhận không khớp");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng"));

        boolean isOAuthSession = SecurityUtils.getCurrentUser()
                .map(UserPrincipal::isOAuth2User)
                .orElse(false);

        // Với tài khoản thường (không phải OAuth), bắt buộc phải có mật khẩu hiện tại và kiểm tra khớp
        if (!isOAuthSession) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new AppException("Vui lòng nhập mật khẩu hiện tại");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new AppException("Mật khẩu hiện tại không chính xác");
            }
        } else if (request.getCurrentPassword() != null && !request.getCurrentPassword().isBlank()) {
            // Nếu người dùng OAuth chủ động nhập mật khẩu hiện tại (đã từng đổi mật khẩu trước đó)
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new AppException("Mật khẩu hiện tại không chính xác");
            }
        }

        // Xác thực mã OTP 2FA gửi qua Gmail
        if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new AppException("Vui lòng nhập mã OTP xác thực được gửi đến email của bạn");
        }

        boolean valid = otpService.verifyOtp(user.getEmail(), request.getOtpCode().trim(), "CHANGE_PASSWORD");
        if (!valid) {
            throw new AppException("Mã OTP không chính xác hoặc đã hết hạn (hiệu lực 10 phút)");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("User {} successfully updated password with 2FA OTP verification (isOAuth={})", user.getUsername(), isOAuthSession);
    }

    @Transactional
    public String updateAvatar(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng"));

        IStorageService.StorageResult result = storageService.uploadFile(file, "avatars");
        if (!result.isSuccess()) {
            throw new AppException("Tải ảnh thất bại: " + result.errorMessage());
        }

        user.setAvatarUrl(result.publicUrl());
        userRepository.save(user);
        return result.publicUrl();
    }

    /**
     * Lấy avatar URL của user
     * @param userId User ID
     * @return Avatar URL hoặc null nếu không có
     */
    @Transactional(readOnly = true)
    public String getUserAvatarUrl(Long userId) {
        return userRepository.findById(userId)
                .map(User::getAvatarUrl)
                .orElse(null);
    }
}

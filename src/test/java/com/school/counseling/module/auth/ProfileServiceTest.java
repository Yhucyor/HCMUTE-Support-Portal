package com.school.counseling.module.auth;

import com.school.counseling.common.exception.AppException;
import com.school.counseling.common.storage.IStorageService;
import com.school.counseling.module.auth.dto.ChangePasswordRequest;
import com.school.counseling.module.auth.dto.UpdateProfileRequest;
import com.school.counseling.module.auth.dto.UserProfileDto;
import com.school.counseling.module.auth.entity.Role;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.UserStatus;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.auth.service.OtpService;
import com.school.counseling.module.auth.service.ProfileService;
import com.school.counseling.module.notification.service.EmailAsyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UNIT TEST - MODULE AUTH: Kiểm thử ProfileService (Cập nhật hồ sơ & Đổi mật khẩu 2FA)")
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IStorageService storageService;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailAsyncService emailAsyncService;

    @InjectMocks
    private ProfileService profileService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().id(1L).name("ROLE_STUDENT").description("Sinh viên").build();
        sampleUser = User.builder()
                .id(10L)
                .username("student10")
                .fullName("Nguyen Van B")
                .email("student10@hcmute.edu.vn")
                .passwordHash("old_encoded_hash")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
    }

    @Test
    @DisplayName("Cập nhật thông tin cá nhân thành công")
    void testUpdateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Nguyen Van B Moi");
        request.setEmail("student10_new@hcmute.edu.vn");
        request.setPhone("0912345678");

        when(userRepository.existsByEmail("student10_new@hcmute.edu.vn")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileDto result = profileService.updateProfile(10L, request);

        assertNotNull(result);
        assertEquals("Nguyen Van B Moi", result.getFullName());
        assertEquals("student10_new@hcmute.edu.vn", result.getEmail());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Cập nhật thông tin thất bại: Email mới trùng với tài khoản khác")
    void testUpdateProfile_EmailDuplicate_ThrowsException() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Nguyen Van B");
        request.setEmail("another_user@hcmute.edu.vn");

        when(userRepository.existsByEmail("another_user@hcmute.edu.vn")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () ->
                profileService.updateProfile(10L, request));
        assertTrue(ex.getMessage().contains("Email đã được sử dụng"));
    }

    @Test
    @DisplayName("Đổi mật khẩu thành công với xác thực OTP 2FA")
    void testChangePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass@123");
        request.setNewPassword("NewPass@456");
        request.setConfirmPassword("NewPass@456");
        request.setOtpCode("998877");

        when(passwordEncoder.matches("OldPass@123", "old_encoded_hash")).thenReturn(true);
        when(otpService.verifyOtp("student10@hcmute.edu.vn", "998877", "CHANGE_PASSWORD")).thenReturn(true);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("new_hash_456");

        profileService.changePassword(10L, request);

        assertEquals("new_hash_456", sampleUser.getPasswordHash());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại: Mật khẩu hiện tại không chính xác")
    void testChangePassword_WrongCurrentPassword_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("WrongPass");
        request.setNewPassword("NewPass@456");
        request.setConfirmPassword("NewPass@456");
        request.setOtpCode("998877");

        when(passwordEncoder.matches("WrongPass", "old_encoded_hash")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () ->
                profileService.changePassword(10L, request));
        assertTrue(ex.getMessage().contains("Mật khẩu hiện tại không chính xác"));
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại: Mã OTP không chính xác hoặc hết hạn")
    void testChangePassword_InvalidOtp_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass@123");
        request.setNewPassword("NewPass@456");
        request.setConfirmPassword("NewPass@456");
        request.setOtpCode("000000");

        when(passwordEncoder.matches("OldPass@123", "old_encoded_hash")).thenReturn(true);
        when(otpService.verifyOtp("student10@hcmute.edu.vn", "000000", "CHANGE_PASSWORD")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () ->
                profileService.changePassword(10L, request));
        assertTrue(ex.getMessage().contains("Mã OTP không chính xác hoặc đã hết hạn"));
    }
}

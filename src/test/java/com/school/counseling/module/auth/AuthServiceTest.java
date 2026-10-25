package com.school.counseling.module.auth;

import com.school.counseling.common.exception.AppException;
import com.school.counseling.module.academic.service.HcmuteCrawlerService;
import com.school.counseling.module.auth.dto.LoginRequest;
import com.school.counseling.module.auth.dto.RegisterRequest;
import com.school.counseling.module.auth.dto.UserProfileDto;
import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.auth.entity.Role;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.UserStatus;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.RoleRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.auth.service.AuthService;
import com.school.counseling.module.auth.service.JwtTokenProvider;
import com.school.counseling.module.auth.service.OtpService;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UNIT TEST - MODULE AUTH: Kiểm thử xác thực, đăng ký và bảo mật tài khoản")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailAsyncService emailAsyncService;

    @Mock
    private HcmuteCrawlerService hcmuteCrawlerService;

    @InjectMocks
    private AuthService authService;

    private Role studentRole;

    @BeforeEach
    void setUp() {
        studentRole = Role.builder().id(1L).name("ROLE_STUDENT").description("Sinh viên HCMUTE").build();
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
    }

    @Test
    @DisplayName("Đăng ký thành công: Tạo tài khoản ở trạng thái PENDING_ACTIVATION và gửi OTP kích hoạt")
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("student123");
        request.setPassword("Password@123");
        request.setConfirmPassword("Password@123");
        request.setFullName("Nguyen Van A");
        request.setEmail("student123@gmail.com");
        request.setPhone("0987654321");

        when(userRepository.existsByUsername("student123")).thenReturn(false);
        when(userRepository.existsByEmail("student123@gmail.com")).thenReturn(false);
        when(otpService.generateOtp(eq("student123@gmail.com"), eq("ACCOUNT_ACTIVATION"))).thenReturn("123456");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserProfileDto result = authService.register(request);

        assertNotNull(result);
        assertEquals("student123", result.getUsername());
        assertEquals(UserStatus.PENDING_ACTIVATION, result.getStatus());
        verify(emailAsyncService, times(1)).sendOtpEmail(eq("student123@gmail.com"), anyString(), eq("123456"), anyString());
    }

    @Test
    @DisplayName("Đăng ký thất bại: Mật khẩu xác nhận không trùng khớp")
    void testRegister_PasswordMismatch_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("student123");
        request.setPassword("Password@123");
        request.setConfirmPassword("DifferentPassword");

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("không khớp"));
    }

    @Test
    @DisplayName("Đăng ký thất bại: Tên đăng nhập đã tồn tại")
    void testRegister_DuplicateUsername_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("student_existed");
        request.setPassword("Password@123");
        request.setConfirmPassword("Password@123");

        when(userRepository.existsByUsername("student_existed")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Tên đăng nhập đã được sử dụng"));
    }

    @Test
    @DisplayName("Kích hoạt tài khoản thành công khi OTP hợp lệ")
    void testActivateAccount_Success() {
        String email = "student@gmail.com";
        String otp = "654321";
        User user = User.builder()
                .id(1L)
                .username("student")
                .email(email)
                .status(UserStatus.PENDING_ACTIVATION)
                .build();

        when(otpService.verifyOtp(email, otp, "ACCOUNT_ACTIVATION")).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        authService.activateAccount(email, otp);

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Kích hoạt tài khoản thất bại: OTP sai hoặc hết hạn")
    void testActivateAccount_InvalidOtp_ThrowsException() {
        String email = "student@gmail.com";
        String otp = "000000";

        when(otpService.verifyOtp(email, otp, "ACCOUNT_ACTIVATION")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> authService.activateAccount(email, otp));
        assertTrue(ex.getMessage().contains("không chính xác hoặc đã hết hạn"));
    }

    @Test
    @DisplayName("Đặt lại mật khẩu: Cập nhật password mới khi OTP PASSWORD_RESET hợp lệ")
    void testResetPassword_Success() {
        String email = "user@gmail.com";
        String otp = "112233";
        String newPassword = "NewPassword@123";

        User user = User.builder()
                .id(2L)
                .username("user2")
                .email(email)
                .passwordHash("old_hash")
                .build();

        when(otpService.verifyOtp(email, otp, "PASSWORD_RESET")).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("new_encoded_hash");

        authService.resetPassword(email, otp, newPassword);

        assertEquals("new_encoded_hash", user.getPasswordHash());
        verify(userRepository, times(1)).save(user);
    }
}

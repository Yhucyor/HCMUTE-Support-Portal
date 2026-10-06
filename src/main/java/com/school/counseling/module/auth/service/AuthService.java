package com.school.counseling.module.auth.service;

import com.school.counseling.common.exception.AppException;
import com.school.counseling.module.auth.dto.AuthResponse;
import com.school.counseling.module.auth.dto.LoginRequest;
import com.school.counseling.module.auth.dto.RegisterRequest;
import com.school.counseling.module.auth.dto.UserProfileDto;
import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.Role;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.UserStatus;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.RoleRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final OtpService otpService;

    @Transactional
    public UserProfileDto register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Mật khẩu xác nhận không khớp");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException("Tên đăng nhập đã được sử dụng");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email đã được đăng ký trong hệ thống");
        }

        Role defaultRole = roleRepository.findByName("ROLE_STUDENT")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_STUDENT")
                        .description("Sinh viên HCMUTE")
                        .build()));

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        }

        User user = User.builder()
                .username(request.getUsername().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone())
                .role(defaultRole)
                .department(department)
                .status(UserStatus.ACTIVE)
                .avatarUrl("https://ui-avatars.com/api/?name=" + request.getFullName().replace(" ", "+") + "&background=0D8ABC&color=fff")
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user: username={}, role={}", savedUser.getUsername(), defaultRole.getName());
        return UserProfileDto.fromEntity(savedUser);
    }

    public AuthResponse authenticate(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String jwt = tokenProvider.generateToken(authentication);

            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new AppException("Không tìm thấy thông tin tài khoản"));

            return AuthResponse.builder()
                    .accessToken(jwt)
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getExpirationMs() / 1000)
                    .user(UserProfileDto.fromEntity(user))
                    .build();
        } catch (BadCredentialsException ex) {
            throw new AppException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Email chưa được đăng ký trong hệ thống"));

        String otp = otpService.generateOtp(user.getEmail(), "PASSWORD_RESET");
        log.info("Password reset OTP generated for {}: {}", email, otp);
    }

    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        boolean valid = otpService.verifyOtp(email, otpCode, "PASSWORD_RESET");
        if (!valid) {
            throw new AppException("Mã OTP không chính xác hoặc đã hết hạn");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password successfully reset for user: {}", user.getUsername());
    }
}

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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
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
    private final com.school.counseling.module.notification.service.EmailAsyncService emailAsyncService;
    private final com.school.counseling.module.academic.service.HcmuteCrawlerService hcmuteCrawlerService;

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
                .status(UserStatus.PENDING_ACTIVATION)
                .avatarUrl("https://ui-avatars.com/api/?name=" + request.getFullName().replace(" ", "+") + "&background=0D8ABC&color=fff")
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user pending activation: username={}, role={}", savedUser.getUsername(), defaultRole.getName());

        // Sinh mã OTP kích hoạt tài khoản 6 chữ số và gửi qua Email bất đồng bộ
        String otp = otpService.generateOtp(savedUser.getEmail(), "ACCOUNT_ACTIVATION");
        log.info("Activation OTP generated for {}: {}", savedUser.getEmail(), otp);
        emailAsyncService.sendOtpEmail(savedUser.getEmail(), savedUser.getFullName(), otp, "kích hoạt tài khoản sinh viên");

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
        } catch (DisabledException ex) {
            throw new AppException("Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email và nhập mã OTP để kích hoạt tài khoản.");
        } catch (LockedException ex) {
            throw new AppException("Tài khoản đã bị tạm khóa do vi phạm quy định.");
        }
    }

    @Transactional
    public void activateAccount(String email, String otpCode) {
        boolean valid = otpService.verifyOtp(email, otpCode, "ACCOUNT_ACTIVATION");
        if (!valid) {
            throw new AppException("Mã OTP kích hoạt không chính xác hoặc đã hết hạn");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Không tìm thấy thông tin tài khoản"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            log.info("Tài khoản {} đã được kích hoạt trước đó", user.getUsername());
            return;
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("Account successfully activated for user: {}", user.getUsername());
    }

    @Transactional
    public void resendActivationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Email chưa được đăng ký trong hệ thống"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new AppException("Tài khoản này đã được kích hoạt trước đó. Vui lòng đăng nhập.");
        }

        String otp = otpService.generateOtp(user.getEmail(), "ACCOUNT_ACTIVATION");
        log.info("Resent activation OTP for {}: {}", email, otp);
        emailAsyncService.sendOtpEmail(user.getEmail(), user.getFullName(), otp, "kích hoạt tài khoản sinh viên");
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Email chưa được đăng ký trong hệ thống"));

        String otp = otpService.generateOtp(user.getEmail(), "PASSWORD_RESET");
        log.info("Password reset OTP generated for {}: {}", email, otp);
        emailAsyncService.sendOtpEmail(user.getEmail(), user.getFullName(), otp, "đặt lại mật khẩu tài khoản");
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

    @Transactional
    public User loginWithHcmutePortal(String studentId, String password, jakarta.servlet.http.HttpServletRequest request) {
        if (studentId == null || studentId.isBlank() || password == null || password.isBlank()) {
            throw new AppException("Vui lòng nhập đầy đủ Mã số sinh viên và Mật khẩu Cổng đào tạo.");
        }

        studentId = studentId.trim();
        password = password.trim();

        // 1. Xác thực với Cổng đào tạo HCMUTE
        com.fasterxml.jackson.databind.JsonNode authResult;
        try {
            authResult = hcmuteCrawlerService.authenticatePortal(studentId, password);
        } catch (Exception e) {
            log.warn("Đăng nhập HCMUTE Portal thất bại cho MSSV {}: {}", studentId, e.getMessage());
            throw new AppException(e.getMessage() != null ? e.getMessage() : "Mã số sinh viên hoặc mật khẩu Cổng đào tạo không chính xác!");
        }

        String fullName = (authResult != null && authResult.hasNonNull("FullName"))
                ? authResult.get("FullName").asText()
                : "Sinh viên " + studentId;
        String email = studentId.toLowerCase() + "@student.hcmute.edu.vn";
        String encodedPortalPassword = java.util.Base64.getEncoder().encodeToString(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // 2. Tìm hoặc tự động tạo tài khoản trong DB
        final String finalStudentId = studentId;
        final String finalFullName = fullName;
        User user = userRepository.findByUsername(studentId)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> {
                    Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                            .orElseGet(() -> roleRepository.save(Role.builder()
                                    .name("ROLE_STUDENT")
                                    .description("Sinh viên HCMUTE")
                                    .build()));

                    User newUser = User.builder()
                            .username(finalStudentId)
                            .passwordHash(passwordEncoder.encode(password))
                            .fullName(finalFullName)
                            .email(email)
                            .role(studentRole)
                            .status(UserStatus.ACTIVE)
                            .avatarUrl("https://ui-avatars.com/api/?name=" + finalFullName.replace(" ", "+") + "&background=0D8ABC&color=fff")
                            .portalUsername(finalStudentId)
                            .portalPasswordEnc(encodedPortalPassword)
                            .build();

                    log.info("Tự động tạo tài khoản sinh viên từ Cổng Đào tạo HCMUTE: MSSV={}", finalStudentId);
                    return userRepository.save(newUser);
                });

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException("Tài khoản sinh viên đã bị tạm khóa do vi phạm quy định.");
        }

        // Tự kích hoạt nếu đang chờ
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        user.setPortalUsername(studentId);
        user.setPortalPasswordEnc(encodedPortalPassword);
        user = userRepository.save(user);

        // 3. Khởi tạo Security Context đăng nhập phiên làm việc
        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (request != null) {
            jakarta.servlet.http.HttpSession session = request.getSession(true);
            session.setAttribute(
                    org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );
        }

        // 4. Kích hoạt đồng bộ dữ liệu học vụ (Điểm, TKB, CTĐT) chạy ngầm
        final String sId = studentId;
        final String pwd = password;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                hcmuteCrawlerService.syncStudentData(new com.school.counseling.module.academic.dto.HcmuteSyncRequest(sId, pwd));
            } catch (Exception ex) {
                log.warn("Đồng bộ dữ liệu học vụ ngầm cho MSSV {} thất bại: {}", sId, ex.getMessage());
            }
        });

        return user;
    }
}

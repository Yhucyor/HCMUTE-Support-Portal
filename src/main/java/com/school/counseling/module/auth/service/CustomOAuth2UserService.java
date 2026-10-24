package com.school.counseling.module.auth.service;

import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.auth.entity.Role;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.UserStatus;
import com.school.counseling.module.auth.repository.RoleRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (OAuth2AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Lỗi xử lý đăng nhập Google OAuth2: ", ex);
            throw new OAuth2AuthenticationException(new OAuth2Error("server_error"), ex.getMessage());
        }
    }

    public UserPrincipal processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_email"), "Không tìm thấy địa chỉ email từ tài khoản Google.");
        }

        // Ưu tiên trích xuất Họ Tên đầy đủ từ Google nếu trường name rỗng
        if (name == null || name.isBlank()) {
            if (givenName != null || familyName != null) {
                name = ((familyName != null ? familyName + " " : "") + (givenName != null ? givenName : "")).trim();
            }
        }

        email = email.trim().toLowerCase();
        log.info("Xử lý xác thực Google OAuth2 cho email: {}", email);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // Kiểm tra trạng thái tài khoản
            if (user.getStatus() == UserStatus.LOCKED) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("user_locked"), "Tài khoản của bạn đã bị tạm khóa. Vui lòng liên hệ Quản trị viên.");
            }

            // Nếu tài khoản đang chờ kích hoạt nhưng đăng nhập qua Google thì tự động kích hoạt
            if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
                user.setStatus(UserStatus.ACTIVE);
            }

            // Cập nhật họ tên nếu chưa có hoặc đang trùng với mã số / username
            if (name != null && !name.isBlank()) {
                if (user.getFullName() == null || user.getFullName().isBlank() || user.getFullName().equalsIgnoreCase(user.getUsername())) {
                    user.setFullName(name.trim());
                }
            }

            // Cập nhật ảnh đại diện nếu chưa có
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && picture != null) {
                user.setAvatarUrl(picture);
            }

            userRepository.save(user);
        } else {
            // Tự động đăng ký tài khoản mới cho sinh viên
            user = registerNewOAuth2User(email, name, picture);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    public UserPrincipal processOidcUser(org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest userRequest,
                                         org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
        UserPrincipal principal = processOAuth2User(userRequest, oidcUser);
        User user = userRepository.findById(principal.getId()).orElse(null);
        return UserPrincipal.create(user, oidcUser.getAttributes(), oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private User registerNewOAuth2User(String email, String name, String picture) {
        // Sinh username từ email (ví dụ: 22110001@student.hcmute.edu.vn -> 22110001)
        String baseUsername = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9._-]", "");
        if (baseUsername.isBlank()) {
            baseUsername = "user" + System.currentTimeMillis();
        }

        String username = baseUsername;
        int count = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + count;
            count++;
        }

        // Mặc định gán Role ROLE_STUDENT cho sinh viên
        Role defaultRole = roleRepository.findByName("ROLE_STUDENT")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_STUDENT")
                        .description("Sinh viên HCMUTE")
                        .build()));

        String fullName = (name != null && !name.isBlank()) ? name.trim() : username;
        String avatarUrl = (picture != null && !picture.isBlank()) 
                ? picture 
                : "https://ui-avatars.com/api/?name=" + fullName.replace(" ", "+") + "&background=0D8ABC&color=fff";

        User newUser = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) // Mật khẩu ngẫu nhiên an toàn cho OAuth2
                .fullName(fullName)
                .email(email)
                .avatarUrl(avatarUrl)
                .role(defaultRole)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Tự động tạo tài khoản sinh viên qua Google OAuth2: username={}, email={}", savedUser.getUsername(), savedUser.getEmail());
        return savedUser;
    }
}

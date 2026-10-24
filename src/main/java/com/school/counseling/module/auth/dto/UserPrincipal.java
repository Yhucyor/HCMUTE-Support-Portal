package com.school.counseling.module.auth.dto;

import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails, OAuth2User, OidcUser {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String avatarUrl;
    private final String role;
    private final Long departmentId;
    private final String departmentCode;
    private final String departmentName;
    private final UserStatus status;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public static UserPrincipal create(User user) {
        return create(user, Collections.emptyMap());
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        String roleName = user.getRole() != null ? user.getRole().getName() : "ROLE_STUDENT";
        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);

        Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        String deptCode = user.getDepartment() != null ? user.getDepartment().getCode() : null;
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : null;

        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(roleName)
                .departmentId(deptId)
                .departmentCode(deptCode)
                .departmentName(deptName)
                .status(user.getStatus())
                .authorities(Collections.singletonList(authority))
                .attributes(attributes != null ? attributes : Collections.emptyMap())
                .build();
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes, OidcIdToken idToken, OidcUserInfo userInfo) {
        UserPrincipal principal = create(user, attributes);
        return UserPrincipal.builder()
                .id(principal.getId())
                .username(principal.getUsername())
                .password(principal.getPassword())
                .fullName(principal.getFullName())
                .email(principal.getEmail())
                .phone(principal.getPhone())
                .avatarUrl(principal.getAvatarUrl())
                .role(principal.getRole())
                .departmentId(principal.getDepartmentId())
                .departmentCode(principal.getDepartmentCode())
                .departmentName(principal.getDepartmentName())
                .status(principal.getStatus())
                .authorities(principal.getAuthorities())
                .attributes(principal.getAttributes())
                .idToken(idToken)
                .userInfo(userInfo)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getClaims() {
        return idToken != null ? idToken.getClaims() : (attributes != null ? attributes : Collections.emptyMap());
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    @Override
    public String getName() {
        return (fullName != null && !fullName.isBlank()) ? fullName : (username != null ? username : email);
    }
}

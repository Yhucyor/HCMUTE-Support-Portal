package com.school.counseling.module.auth.dto;

import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails {

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

    public static UserPrincipal create(User user) {
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
}

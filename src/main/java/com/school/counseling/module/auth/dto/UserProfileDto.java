package com.school.counseling.module.auth.dto;

import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String portalUsername;   // pre-fill form liên kết Portal
    private boolean portalLinked;    // true nếu đã lưu credentials
    private String role;
    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private UserStatus status;
    private LocalDateTime createdAt;

    public static UserProfileDto fromEntity(User user) {
        if (user == null) return null;
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .portalUsername(user.getPortalUsername())
                .portalLinked(user.getPortalUsername() != null && !user.getPortalUsername().isBlank()
                        && user.getPortalPasswordEnc() != null && !user.getPortalPasswordEnc().isBlank())
                .role(user.getRole() != null ? user.getRole().getName().replace("ROLE_", "") : "STUDENT")
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .departmentCode(user.getDepartment() != null ? user.getDepartment().getCode() : null)
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

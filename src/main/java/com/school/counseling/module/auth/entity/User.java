package com.school.counseling.module.auth.entity;

import com.school.counseling.common.entity.BaseEntity;
import jakarta.persistence.*;
// bộ thư viện chuẩn của jpa  dùng để ánh xạ code java xuống csdl trong mysql 
import lombok.*;
import lombok.experimental.SuperBuilder;
// bản nâng cấp của @Builder trong Lombok dùng khi class có kế thừa extend BaseEntity 

@Entity // báo cho springboot và hibernate bt hibernate sẽ quản lý class này
@Table(name = "users") // tên bảng dưới db phải là users
@Getter // Lombok tự động tạo ra các hàm đọc getUsername() , getEmail() và gán set
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// baseEntity là class cha chung ở phần entity của commom dùng lại ở mọi bảng
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "portal_username", length = 100)
    private String portalUsername;

    // Lưu mật khẩu Portal mã hoá Base64 (không phải bcrypt — cần giải mã để đồng bộ)
    @Column(name = "portal_password_enc", length = 500)
    private String portalPasswordEnc;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @lombok.Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
}

package com.school.counseling.module.feed.entity;

import com.school.counseling.common.entity.BaseEntity;
import com.school.counseling.module.auth.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * THỰC THỂ LƯỢT THÍCH ĐA HÌNH (POLYMORPHIC LIKE)
 * 
 * Quản lý lượt thích cho cả Bài viết ('POST') và Bình luận ('COMMENT').
 * Đảm bảo 1 User chỉ được thích 1 lần trên 1 đối tượng qua UNIQUE CONSTRAINT(user_id, target_type, target_id).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_target_like", columnNames = {"user_id", "target_type", "target_id"})
})
public class Like extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(name = "target_type", length = 20, nullable = false)
    private String targetType; // 'POST' hoặc 'COMMENT'

    @NotNull
    @Column(name = "target_id", nullable = false)
    private Long targetId;
}

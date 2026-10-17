package com.school.counseling.module.feed.entity;

import com.school.counseling.common.entity.BaseEntity;
import com.school.counseling.module.auth.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * THỰC THỂ BÁO CÁO VI PHẠM (POST REPORT)
 * 
 * Lưu trữ các báo cáo vi phạm nội dung (spam, ngôn từ kích động, thông tin học vụ sai lệch) từ người dùng.
 * Phục vụ cho phân hệ Kiểm duyệt bài viết (Moderation Service).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "post_reports")
@SQLDelete(sql = "UPDATE post_reports SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PostReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", updatable = false)
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", updatable = false)
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private User reporter;

    @NotBlank(message = "Lý do báo cáo không được để trống")
    @Size(max = 50, message = "Lý do tối đa 50 ký tự")
    @Column(name = "reason", length = 50, nullable = false)
    private String reason; // SPAM, INAPPROPRIATE_LANGUAGE, WRONG_ACADEMIC_INFO, OTHER

    @Size(max = 500, message = "Chi tiết báo cáo tối đa 500 ký tự")
    @Column(name = "details", length = 500)
    private String details;

    @Builder.Default
    @Column(name = "status", length = 30, nullable = false)
    private String status = "PENDING"; // PENDING, RESOLVED, DISMISSED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private User resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "action_taken", length = 50)
    private String actionTaken; // HIDE_POST, DISMISS, LOCK_USER
}

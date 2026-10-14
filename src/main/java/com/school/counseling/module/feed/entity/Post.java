package com.school.counseling.module.feed.entity;

import com.school.counseling.common.entity.BaseEntity;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * THỰC THỂ BÀI VIẾT (POST)
 * 
 * Đại diện cho 2 phân hệ:
 * 1. Bảng tin thông báo chính thức (post_type = 'OFFICIAL_ANNOUNCEMENT') từ các Khoa/Phòng.
 * 2. Diễn đàn thảo luận sinh viên (post_type = 'STUDENT_FORUM') cần kiểm duyệt trước khi hiển thị (status = 'PENDING_APPROVAL').
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "posts")
@SQLDelete(sql = "UPDATE posts SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Post extends BaseEntity {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Builder.Default
    @Column(name = "post_type", length = 30, nullable = false)
    private String postType = "OFFICIAL_ANNOUNCEMENT"; // OFFICIAL_ANNOUNCEMENT, STUDENT_FORUM

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private User author;

    @Builder.Default
    @Column(name = "status", length = 30, nullable = false)
    private String status = "APPROVED"; // PENDING_APPROVAL, APPROVED, REJECTED, HIDDEN

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Builder.Default
    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    @Builder.Default
    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Builder.Default
    @Column(name = "video_status", length = 20)
    private String videoStatus = "NONE"; // NONE, PROCESSING, COMPLETED, FAILED

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Builder.Default
    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostAttachment> attachments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Comment> comments = new ArrayList<>();

    public void addAttachment(PostAttachment attachment) {
        attachments.add(attachment);
        attachment.setPost(this);
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setPost(this);
    }
}

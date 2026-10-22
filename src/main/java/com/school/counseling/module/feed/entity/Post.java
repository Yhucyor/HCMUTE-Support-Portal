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
 * 1. Bảng tin thông báo chính thức (post_type = 'OFFICIAL_ANNOUNCEMENT') từ các
 * Khoa/Phòng.
 * 2. Diễn đàn thảo luận sinh viên (post_type = 'STUDENT_FORUM') cần kiểm duyệt
 * trước khi hiển thị (status = 'PENDING_APPROVAL').
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "posts")
// SQLDelete và SQLRestriction thường được sử dụng cùng nhau
// để triển khai cơ chế xóa mềm (soft delete)
// ghi đề câu lệnh delete mặc định của hibernate
// vd gọi postRepository.delete(post) or deleteByID(id)
// sẽ không thực sự xóa bản ghi trong database, thay vào đó chỉ cập nhật lại cột
// is_deleted = true
// SQLRestriction("is_deleted = false")
// giới hạn truy vấn dữ liệu
// trong các truy vấn của jpa repository sẽ tự động thêm điều kiện is_deleted =
// false
// vd: postRepository.findAll() sẽ truy vấn là select * from posts where
// is_deleted = false
// giúp khôi phục dữ liệu dễ dàng nếu cần thiết
@SQLDelete(sql = "UPDATE posts SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Post extends BaseEntity {

    // đọc qua cơ chế reflection nên đảo thứ tự ko sao
    // reflection là 1 kỹ thuật của java cho phép inspect (kiểm tra) và mutate (thay
    // đổi)
    // một lớp/biến/method tại thời điểm runtime (thời gian chạy)
    // vd: @ lombok.Data, @Builder,... sử dụng reflection để tự động sinh ra code
    //

    // bean validation @notblank , @size @ email , nhiệm vụ kt hợp lệ ở tầng appli
    // java
    // chặn ngay trong ram trc khi viết sql
    // lõoi return methodArgumentNotValiExceptin
    // or contrainviolationException http 400 bad request rõ ràng ,thân thiện

    // jpa / hibernate @column , @entity @table ánh xạ đối tượng ORM Mapping và định
    // nghĩa tccasu truc duoi db DDL
    // return DataIntergrityViolateionExcep / SQLException http 500 internal server
    // error nặng nề

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

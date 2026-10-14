package com.school.counseling.module.feed.entity;

import com.school.counseling.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * THỰC THỂ TỆP ĐÍNH KÈM BÀI VIẾT (POST ATTACHMENT)
 * 
 * Lưu trữ danh sách file (PDF thông báo, hình ảnh, video minh họa) gắn với bài viết.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "post_attachments")
@SQLDelete(sql = "UPDATE post_attachments SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PostAttachment extends BaseEntity {

    @NotNull(message = "Bài viết không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @NotBlank(message = "Tên tệp không được để trống")
    @Size(max = 255, message = "Tên tệp tối đa 255 ký tự")
    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @NotBlank(message = "Đường dẫn tệp không được để trống")
    @Size(max = 500, message = "Đường dẫn tệp tối đa 500 ký tự")
    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @NotBlank(message = "Loại tệp không được để trống")
    @Size(max = 50, message = "Loại tệp tối đa 50 ký tự")
    @Column(name = "file_type", length = 50, nullable = false)
    private String fileType; // PDF, DOCX, XLSX, MP4, IMAGE

    @Builder.Default
    @Column(name = "file_size", nullable = false)
    private Long fileSize = 0L;

    @Builder.Default
    @Column(name = "source_type", length = 30)
    private String sourceType = "DIRECT_UPLOAD"; // DIRECT_UPLOAD, NODEJS_WEBHOOK
}

package com.school.counseling.module.admin.entity;

import com.school.counseling.common.entity.BaseEntity;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "internal_staff_admin_messages", indexes = {
    @Index(name = "idx_internal_msg_dept_created", columnList = "department_id, created_at"),
    @Index(name = "idx_internal_msg_sender_created", columnList = "sender_id, created_at")
})
@SQLDelete(sql = "UPDATE internal_staff_admin_messages SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class InternalStaffAdminMessage extends BaseEntity {

    @NotNull(message = "Người gửi không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    @Column(name = "subject", length = 255)
    private String subject;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "message_type", length = 50, nullable = false)
    private String messageType = "INQUIRY";

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
}

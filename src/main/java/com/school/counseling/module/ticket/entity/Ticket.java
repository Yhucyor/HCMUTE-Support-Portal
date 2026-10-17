package com.school.counseling.module.ticket.entity;

import com.school.counseling.common.entity.BaseEntity;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.chat.entity.Attachment;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_tickets_dept_status_due", columnList = "department_id, status, due_date")
})
@SQLDelete(sql = "UPDATE tickets SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Ticket extends BaseEntity {

    @NotBlank(message = "Mã ticket không được để trống")
    @Size(max = 50, message = "Mã ticket tối đa 50 ký tự")
    @Column(name = "ticket_code", length = 50, nullable = false, unique = true)
    private String ticketCode;

    @NotBlank(message = "Tiêu đề yêu cầu không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @NotBlank(message = "Mô tả yêu cầu không được để trống")
    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @NotNull(message = "Đơn vị xử lý không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator; // Null nếu là Guest

    @Size(max = 150, message = "Họ tên Guest tối đa 150 ký tự")
    @Column(name = "guest_name", length = 150)
    private String guestName;

    @Size(max = 150, message = "Email Guest tối đa 150 ký tự")
    @Column(name = "guest_email", length = 150)
    private String guestEmail;

    @Size(max = 100, message = "Token tra cứu tối đa 100 ký tự")
    @Column(name = "guest_token", length = 100)
    private String guestToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo; // Staff tiếp nhận

    @Builder.Default
    @Column(name = "priority", length = 20, nullable = false)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT

    @Column(name = "topic_category", length = 150)
    private String topicCategory;

    @Builder.Default
    @Column(name = "status", length = 30, nullable = false)
    private String status = "OPEN"; // OPEN, IN_PROGRESS, WAITING_STUDENT, RESOLVED, CLOSED, OVERDUE

    @NotNull(message = "Thời hạn xử lý (due_date) không được để trống")
    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "rating")
    private Integer rating; // 1 - 5 sao

    @Builder.Default
    @Column(name = "has_student_reply")
    private Boolean hasStudentReply = false;

    @Builder.Default
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<TicketHistory> histories = new ArrayList<>();
}

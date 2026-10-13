package com.school.counseling.module.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSummaryDto {

    private Long id;
    private String ticketCode;
    private String title;

    private Long departmentId;
    private String departmentName;

    private String status;
    private String priority;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;

    private String creatorOrGuestName;
    private String assignedStaffName;
    private Boolean hasStudentReply;
    private Integer rating;

    public TicketSummaryDto(Long id, String ticketCode, String title,
                            Long departmentId, String departmentName,
                            String status, String priority, LocalDateTime dueDate, LocalDateTime createdAt,
                            String creatorOrGuestName, String assignedStaffName) {
        this(id, ticketCode, title, departmentId, departmentName, status, priority, dueDate, createdAt, creatorOrGuestName, assignedStaffName, false, null);
    }

    public TicketSummaryDto(Long id, String ticketCode, String title,
                            Long departmentId, String departmentName,
                            String status, String priority, LocalDateTime dueDate, LocalDateTime createdAt,
                            String creatorOrGuestName, String assignedStaffName, Boolean hasStudentReply) {
        this(id, ticketCode, title, departmentId, departmentName, status, priority, dueDate, createdAt, creatorOrGuestName, assignedStaffName, hasStudentReply, null);
    }

    public boolean isHasStudentReply() {
        return Boolean.TRUE.equals(hasStudentReply);
    }

    public boolean getHasStudentReply() {
        return Boolean.TRUE.equals(hasStudentReply);
    }

    public boolean isOverdue() {
        return dueDate != null
                && LocalDateTime.now().isAfter(dueDate)
                && !"RESOLVED".equals(status)
                && !"CLOSED".equals(status);
    }

    public boolean getIsOverdue() {
        return isOverdue();
    }

    public boolean isDueSoon() {
        if (dueDate == null || "RESOLVED".equals(status) || "CLOSED".equals(status)) return false;
        return LocalDateTime.now().isBefore(dueDate)
                && LocalDateTime.now().isAfter(dueDate.minusHours(24));
    }

    public boolean getIsDueSoon() {
        return isDueSoon();
    }
}
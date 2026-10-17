package com.school.counseling.module.admin.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalMessageDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private String senderDepartmentName;
    private Long receiverId;
    private String receiverName;
    private Long departmentId;
    private String departmentName;
    private String subject;
    private String content;
    private String messageType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

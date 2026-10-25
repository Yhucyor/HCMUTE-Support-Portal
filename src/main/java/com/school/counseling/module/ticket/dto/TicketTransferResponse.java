package com.school.counseling.module.ticket.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTransferResponse {

    private Long id;
    private Long ticketId;
    private String ticketCode;
    private Long fromDepartmentId;
    private String fromDepartmentName;
    private Long toDepartmentId;
    private String toDepartmentName;
    private Long transferredById;
    private String transferredByName;
    private String reason;
    private LocalDateTime transferredAt;
}

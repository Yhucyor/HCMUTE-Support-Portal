package com.school.counseling.module.ticket.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketReplyRequest {

    private String content;
    private String newStatus;
    private String actionNote;
}
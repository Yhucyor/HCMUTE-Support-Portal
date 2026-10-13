package com.school.counseling.module.ticket.event;

/**
 * Domain Event: Phát ra khi một Ticket được giải quyết (RESOLVED) và cần gửi email kết quả.
 */
public record TicketResolvedEvent(
        Long ticketId,
        String ticketCode,
        String recipientEmail,
        String recipientName,
        String replyContent,
        String trackingUrl
) {}

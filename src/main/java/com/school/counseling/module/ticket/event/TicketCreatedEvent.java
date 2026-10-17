package com.school.counseling.module.ticket.event;

import java.time.LocalDateTime;

/**
 * Domain Event: Phát ra khi một Ticket mới được tạo thành công trong hệ thống.
 * Giúp tách rời (decouple) logic nghiệp vụ Ticket khỏi logic gửi email thông báo.
 */
public record TicketCreatedEvent(
        Long ticketId,
        String ticketCode,
        String title,
        String priority,
        LocalDateTime dueDate,
        String recipientEmail,
        String recipientName,
        String trackingUrl
) {}

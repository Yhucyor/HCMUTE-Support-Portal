package com.school.counseling.module.ticket.event;

/**
 * Domain Event: Phát ra khi một Cán bộ tiếp nhận xử lý thành công một Ticket (Atomic Claimed).
 */
public record TicketClaimedEvent(
        Long ticketId,
        String ticketCode,
        String recipientEmail,
        String recipientName,
        String staffName,
        String departmentName,
        String trackingUrl
) {}

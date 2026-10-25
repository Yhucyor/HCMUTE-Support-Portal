package com.school.counseling.module.ticket.event;

public record TicketTransferredEvent(
        Long ticketId,
        String ticketCode,
        String title,
        String fromDeptName,
        String toDeptName,
        String transferredByName,
        String reason,
        String trackingUrl
) {}

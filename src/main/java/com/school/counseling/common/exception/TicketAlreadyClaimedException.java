package com.school.counseling.common.exception;

/**
 * Ném ra khi một Cán bộ cố tình nhận Ticket đã được Cán bộ khác tiếp nhận trước đó (Race Condition Guard).
 * HTTP Status: 409 Conflict
 */
public class TicketAlreadyClaimedException extends RuntimeException {

    public TicketAlreadyClaimedException(String message) {
        super(message);
    }
}

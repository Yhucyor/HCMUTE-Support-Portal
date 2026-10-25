package com.school.counseling.module.ai.entity;

/**
 * Trạng thái của AI Conversation
 */
public enum ConversationStatus {
    /**
     * Đang hoạt động, có thể tiếp tục chat
     */
    ACTIVE,
    
    /**
     * Đã chuyển thành Ticket
     */
    CONVERTED_TO_TICKET,
    
    /**
     * Đã lưu trữ (archived)
     */
    ARCHIVED,
    
    /**
     * Bị đóng do vi phạm policy
     */
    CLOSED
}

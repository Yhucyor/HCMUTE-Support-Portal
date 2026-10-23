package com.school.counseling.module.ai.entity;

/**
 * Vai trò của message trong conversation
 */
public enum MessageRole {
    /**
     * Tin nhắn từ User/Guest (câu hỏi)
     */
    USER,
    
    /**
     * Tin nhắn từ AI Assistant (câu trả lời)
     */
    ASSISTANT,
    
    /**
     * Tin nhắn hệ thống (system prompt, metadata)
     */
    SYSTEM
}

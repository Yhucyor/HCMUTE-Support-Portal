package com.school.counseling.module.ai.entity;

/**
 * Loại feedback từ user về AI response
 */
public enum FeedbackType {
    /**
     * Phản hồi tích cực (👍)
     */
    THUMBS_UP,
    
    /**
     * Phản hồi tiêu cực (👎)
     */
    THUMBS_DOWN,
    
    /**
     * Báo cáo nội dung không phù hợp (🚩)
     */
    REPORT,
    
    /**
     * Câu trả lời không chính xác
     */
    INACCURATE,
    
    /**
     * Câu trả lời không liên quan
     */
    IRRELEVANT,
    
    /**
     * Câu trả lời quá chung chung
     */
    TOO_GENERIC,
    
    /**
     * Câu trả lời thiếu chi tiết
     */
    INSUFFICIENT_DETAIL
}

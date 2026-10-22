package com.school.counseling.module.ai.entity;

import com.school.counseling.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Entity cho từng tin nhắn trong conversation
 * Lưu cả USER message và ASSISTANT response
 */
@Entity
@Table(name = "ai_chat_messages", indexes = {
    @Index(name = "idx_msg_conv_created", columnList = "conversation_id, created_at"),
    @Index(name = "idx_msg_role", columnList = "role")
})
@SQLDelete(sql = "UPDATE ai_chat_messages SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AiChatMessage extends BaseEntity {
    
    /**
     * Conversation cha
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    /**
     * Vai trò của message (USER hoặc ASSISTANT)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;
    
    /**
     * Nội dung tin nhắn
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    /**
     * Sources trích dẫn (JSON array)
     * Chỉ có ở ASSISTANT message
     */
    @Column(name = "sources_json", columnDefinition = "TEXT")
    private String sourcesJson;
    
    /**
     * AI Provider đã sử dụng
     */
    @Column(name = "provider", length = 50)
    private String provider;
    
    /**
     * Độ tin cậy (0.0 - 1.0)
     */
    @Column(name = "confidence")
    private Double confidence;
    
    /**
     * Số token đã sử dụng
     */
    @Column(name = "tokens_used")
    private Integer tokensUsed;
    
    /**
     * Thời gian xử lý (milliseconds)
     */
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    /**
     * Có phải message bị báo cáo không
     */
    @Column(name = "is_flagged")
    private Boolean isFlagged;
    
    /**
     * Lý do báo cáo (nếu có)
     */
    @Column(name = "flag_reason", length = 500)
    private String flagReason;
    
    /**
     * PrePersist: Khởi tạo giá trị mặc định
     */
    @PrePersist
    protected void onMessageCreate() {
        super.onCreate();
        if (this.isFlagged == null) {
            this.isFlagged = false;
        }
    }

    /**
     * Getter chuyển đổi chuỗi sourcesJson thành danh sách Map để Thymeleaf và frontend dễ dàng truy cập
     */
    @Transient
    public java.util.List<java.util.Map<String, Object>> getSources() {
        if (sourcesJson == null || sourcesJson.isBlank()) {
            return java.util.Collections.emptyList();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, Object>> list = mapper.readValue(
                sourcesJson, 
                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {}
            );
            if (list != null) {
                for (java.util.Map<String, Object> map : list) {
                    if (!map.containsKey("title") && map.containsKey("source")) {
                        map.put("title", map.get("source"));
                    }
                }
            }
            return list != null ? list : java.util.Collections.emptyList();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}

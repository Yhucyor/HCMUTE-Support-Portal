package com.school.counseling.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho phản hồi từ AI Chatbot
 * Bao gồm câu trả lời, sources, confidence và suggestion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    
    private String answer; // Câu trả lời từ AI
    
    @JsonProperty("session_id")
    private String sessionId; // UUID conversation
    
    private String provider; // Engine đã sử dụng (gemini, ollama, dify)
    
    private Double confidence; // Độ tin cậy 0.0 - 1.0
    
    private List<SourceDocument> sources; // Danh sách nguồn trích dẫn
    
    @JsonProperty("suggest_ticket")
    @Builder.Default
    private Boolean suggestTicket = false; // Gợi ý tạo Ticket khi confidence thấp
    
    @JsonProperty("ticket_category")
    private String ticketCategory; // Auto-fill Department cho modal Ticket
    
    @JsonProperty("ticket_priority")
    private String ticketPriority; // Gợi ý Priority (URGENT, HIGH, MEDIUM, LOW)
    
    private LocalDateTime timestamp; // Thời điểm sinh câu trả lời
    
    @JsonProperty("token_used")
    private Integer tokenUsed; // Số token tiêu thụ (tracking cost)
    
    @JsonProperty("response_time_ms")
    private Long responseTimeMs; // Thời gian xử lý (ms)
}

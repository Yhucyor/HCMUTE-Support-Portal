package com.school.counseling.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho yêu cầu chat với AI
 * Bao gồm validation và mapping JSON chuẩn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {
    
    @NotBlank(message = "Câu hỏi không được để trống")
    @Size(min = 3, max = 500, message = "Câu hỏi phải từ 3 đến 500 ký tự")
    private String query;
    
    @JsonProperty("session_id")
    @com.fasterxml.jackson.annotation.JsonAlias({"sessionId", "session_id"})
    private String sessionId; // UUID của conversation
    
    @Pattern(regexp = "gemini|ollama|dify|python|default", message = "Provider không hợp lệ")
    @Builder.Default
    private String provider = "gemini"; // Default engine
    
    @JsonProperty("department_id")
    private Long departmentId; // Filter FAQ theo Khoa/Phòng
    
    @JsonProperty("include_history")
    @Builder.Default
    private Boolean includeHistory = false; // Có gửi lịch sử chat không
    
    @JsonProperty("max_history_messages")
    @Builder.Default
    private Integer maxHistoryMessages = 5; // Số tin nhắn context
}

package com.school.counseling.module.ai.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.module.ai.dto.AiChatRequest;
import com.school.counseling.module.ai.dto.AiChatResponse;
import com.school.counseling.module.ai.entity.AiChatMessage;
import com.school.counseling.module.ai.entity.Conversation;
import com.school.counseling.module.ai.service.AiConversationService;
import com.school.counseling.module.auth.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * REST API Controller cho AI Chatbot với Database Persistence
 * Phase 2: Service Layer Refactor - Tích hợp AiConversationService
 * Phase 3: SSE Streaming Response
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatbotController {

    private final AiConversationService conversationService;

    /**
     * Endpoint chính gửi tin nhắn cho AI (có rate limiting & persistence)
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal User currentUser) {
        
        AiChatResponse response = conversationService.chat(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 🔥 PHASE 3: SSE Streaming Chat Endpoint (giống ChatGPT typing effect)
     */
    @PostMapping("/chat-stream")
    public SseEmitter chatStream(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal User currentUser) {
        
        return conversationService.chatStream(request, currentUser);
    }

    /**
     * Lấy danh sách lịch sử hội thoại của user
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Conversation>>> getConversations(
            @AuthenticationPrincipal User currentUser) {
        
        List<Conversation> conversations = conversationService.getUserConversations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    /**
     * Lấy chi tiết 1 conversation theo sessionId
     */
    @GetMapping("/conversations/{sessionId}")
    public ResponseEntity<ApiResponse<Conversation>> getConversationDetail(
            @PathVariable String sessionId) {
        
        Conversation conversation = conversationService.getConversationBySessionId(sessionId);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    /**
     * Lấy tất cả messages trong conversation
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<AiChatMessage>>> getConversationMessages(
            @PathVariable Long conversationId) {
        
        List<AiChatMessage> messages = conversationService.getConversationMessages(conversationId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * Xóa 1 conversation (soft delete - đổi status ARCHIVED)
     */
    @DeleteMapping("/conversations/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable String sessionId,
            @AuthenticationPrincipal User currentUser) {
        
        conversationService.deleteConversation(sessionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa cuộc hội thoại"));
    }

    /**
     * Chuyển conversation thành Ticket
     */
    @PostMapping("/conversations/{sessionId}/convert-to-ticket")
    public ResponseEntity<ApiResponse<Void>> convertToTicket(
            @PathVariable String sessionId,
            @RequestParam Long ticketId) {
        
        conversationService.convertToTicket(sessionId, ticketId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã chuyển hội thoại thành Ticket"));
    }

    /**
     * 🆕 PHASE 5: Submit feedback cho AI message (Thumbs Up/Down)
     */
    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal User currentUser) {
        
        conversationService.submitFeedback(request.getMessageId(), request.getFeedbackType(), currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã ghi nhận phản hồi của bạn"));
    }

    /**
     * Inner DTO cho Feedback Request
     */
    @lombok.Data
    public static class FeedbackRequest {
        private Long messageId;
        private String feedbackType; // "POSITIVE" or "NEGATIVE"
    }
}

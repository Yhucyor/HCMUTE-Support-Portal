package com.school.counseling.module.ai.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.module.ai.dto.AiChatRequest;
import com.school.counseling.module.ai.dto.AiChatResponse;
import com.school.counseling.module.ai.entity.AiChatMessage;
import com.school.counseling.module.ai.entity.Conversation;
import com.school.counseling.module.ai.service.AiConversationService;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
@Slf4j
public class AiChatbotController {

    private final AiConversationService conversationService;
    private final UserRepository userRepository;

    /**
     * Lấy thực thể User đăng nhập từ SecurityContext (tránh lỗi null do UserPrincipal)
     */
    private User resolveCurrentUser() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElse(null);
    }

    /**
     * Endpoint chính gửi tin nhắn cho AI (có rate limiting & persistence)
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request) {
        
        User currentUser = resolveCurrentUser();
        if (currentUser != null) {
            log.info("✅ AI Chat - Authenticated User: {} (ID: {})", currentUser.getUsername(), currentUser.getId());
        } else {
            log.warn("ℹ️ AI Chat - Guest/Anonymous session (No logged in user)");
        }
        
        AiChatResponse response = conversationService.chat(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 🔥 PHASE 3: SSE Streaming Chat Endpoint (giống ChatGPT typing effect)
     */
    @PostMapping("/chat-stream")
    public SseEmitter chatStream(
            @Valid @RequestBody AiChatRequest request) {
        User currentUser = resolveCurrentUser();
        return conversationService.chatStream(request, currentUser);
    }

    /**
     * Lấy danh sách lịch sử hội thoại của user
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Conversation>>> getConversations() {
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
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
            @PathVariable String sessionId) {
        User currentUser = resolveCurrentUser();
        conversationService.deleteConversation(sessionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa cuộc hội thoại"));
    }

    /**
     * Đổi tiêu đề cuộc hội thoại (Rename Conversation)
     */
    @org.springframework.web.bind.annotation.PatchMapping("/conversations/{sessionId}/title")
    public ResponseEntity<ApiResponse<Conversation>> updateConversationTitle(
            @PathVariable String sessionId,
            @Valid @RequestBody UpdateTitleRequest request) {
        User currentUser = resolveCurrentUser();
        Conversation updated = conversationService.updateConversationTitle(sessionId, request.getTitle(), currentUser);
        return ResponseEntity.ok(ApiResponse.success(updated, "Đã cập nhật tiêu đề cuộc hội thoại"));
    }

    @org.springframework.web.bind.annotation.PutMapping("/conversations/{sessionId}/title")
    public ResponseEntity<ApiResponse<Conversation>> updateConversationTitlePut(
            @PathVariable String sessionId,
            @Valid @RequestBody UpdateTitleRequest request) {
        return updateConversationTitle(sessionId, request);
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
            @RequestBody FeedbackRequest request) {
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng đăng nhập để gửi phản hồi"));
        }
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

    /**
     * Inner DTO cho Update Title Request
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UpdateTitleRequest {
        @jakarta.validation.constraints.NotBlank(message = "Tiêu đề không được để trống")
        @jakarta.validation.constraints.Size(min = 1, max = 255, message = "Tiêu đề tối đa 255 ký tự")
        private String title;
    }
}

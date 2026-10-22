package com.school.counseling.module.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.counseling.common.exception.RateLimitExceededException;
import com.school.counseling.common.exception.ResourceNotFoundException;
import com.school.counseling.module.ai.dto.AiChatRequest;
import com.school.counseling.module.ai.dto.AiChatResponse;
import com.school.counseling.module.ai.dto.SourceDocument;
import com.school.counseling.module.ai.entity.*;
import com.school.counseling.module.ai.repository.AiChatMessageRepository;
import com.school.counseling.module.ai.repository.AiMessageFeedbackRepository;
import com.school.counseling.module.ai.repository.ConversationRepository;
import com.school.counseling.module.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service quản lý Conversation và Message persistence
 * Chịu trách nhiệm lưu trữ và quản lý lịch sử chat
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiConversationService {
    
    private final ConversationRepository conversationRepository;
    private final AiChatMessageRepository messageRepository;
    private final AiMessageFeedbackRepository feedbackRepository; // 🆕 Phase 5
    private final AiChatbotService aiChatbotService; // Delegate AI logic
    private final ObjectMapper objectMapper;
    
    // ExecutorService cho async streaming
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @Value("${app.ai.max-messages-per-day:100}")
    private int maxMessagesPerDay;
    
    @Value("${app.ai.similarity-threshold:0.70}")
    private double similarityThreshold;
    
    @Value("${app.ai.context-window-size:5}")
    private int contextWindowSize;
    
    /**
     * Main entry point: Chat với AI và lưu vào DB
     * 
     * @param request AiChatRequest
     * @param currentUser User đang đăng nhập (có thể null nếu Guest)
     * @return AiChatResponse
     */
    @Transactional
    public AiChatResponse chat(AiChatRequest request, User currentUser) {
        long startTime = System.currentTimeMillis();
        
        // 1. Validate rate limit
        if (currentUser != null) {
            validateRateLimit(currentUser);
        }
        
        // 2. Load hoặc tạo Conversation
        Conversation conversation = getOrCreateConversation(
            request.getSessionId(), 
            currentUser, 
            request.getQuery()
        );
        
        // 3. Lưu User message
        AiChatMessage userMessage = saveUserMessage(conversation, request.getQuery());
        
        // 4. Build context window (5 messages gần nhất)
        List<AiChatMessage> recentMessages = getRecentMessages(conversation.getId(), contextWindowSize);
        
        // 5. Gọi AI Engine (delegate sang AiChatbotService)
        AiChatResponse aiResponse = aiChatbotService.getChatResponse(
            request.getQuery(),
            conversation.getSessionId(),
            request.getProvider()
        );
        
        // 6. Enrich response với metadata
        long responseTime = System.currentTimeMillis() - startTime;
        enrichResponse(aiResponse, responseTime, conversation);
        
        // 7. Lưu Assistant message
        AiChatMessage assistantMessage = saveAssistantMessage(conversation, aiResponse);
        
        // 8. Update conversation metadata
        updateConversationMetadata(conversation);
        
        // 9. Return enriched response
        aiResponse.setSessionId(conversation.getSessionId());
        
        log.info("AI Chat completed - SessionId: {}, ResponseTime: {}ms, Confidence: {}", 
            conversation.getSessionId(), responseTime, aiResponse.getConfidence());
        
        return aiResponse;
    }
    
    /**
     * Validate rate limit (câu hỏi/user/ngày)
     * Miễn trừ cho Quản trị viên (Admin) và Cán bộ (Counselor/Staff)
     */
    private void validateRateLimit(User user) {
        if (user == null) {
            return;
        }

        // Miễn trừ hạn mức cho Admin và Cán bộ tư vấn
        if (user.getRole() != null && user.getRole().getName() != null) {
            String roleName = user.getRole().getName().toUpperCase();
            if (roleName.contains("ADMIN") || roleName.contains("COUNSELOR") || roleName.contains("STAFF")) {
                return;
            }
        }

        LocalDateTime todayStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        Long messageCount = messageRepository.countUserMessagesToday(user.getId(), todayStart);
        
        if (messageCount >= maxMessagesPerDay) {
            log.warn("Rate limit exceeded for userId: {}, count: {}", user.getId(), messageCount);
            throw new RateLimitExceededException(
                String.format("Bạn đã đạt giới hạn %d câu hỏi AI trong ngày hôm nay. Vui lòng thử lại vào ngày mai hoặc tạo Ticket để được Cán bộ hỗ trợ.", maxMessagesPerDay)
            );
        }
    }
    
    /**
     * Load conversation từ DB hoặc tạo mới
     */
    private Conversation getOrCreateConversation(String sessionId, User user, String firstQuery) {
        if (sessionId != null && !sessionId.isBlank()) {
            return conversationRepository.findBySessionId(sessionId)
                .map(existing -> {
                    // Nếu conversation trước đó chưa gắn user (ví dụ guest hoặc bị null), gán user hiện tại vào
                    if (existing.getUser() == null && user != null) {
                        existing.setUser(user);
                        existing.setGuestEmail(null);
                        return conversationRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> createNewConversation(sessionId, user, firstQuery));
        }
        
        // Tạo mới với UUID random
        return createNewConversation(UUID.randomUUID().toString(), user, firstQuery);
    }
    
    /**
     * Tạo conversation mới
     */
    private Conversation createNewConversation(String sessionId, User user, String query) {
        String title = generateTitle(query);
        
        Conversation conversation = Conversation.builder()
            .sessionId(sessionId)
            .title(title)
            .user(user)
            .guestEmail(user == null ? "guest@hcmute.edu.vn" : null)
            .status(ConversationStatus.ACTIVE)
            .messageCount(0)
            .totalTokensUsed(0)
            .build();
        
        Conversation saved = conversationRepository.save(conversation);
        log.info("Created new conversation: {} for user: {}", sessionId, user != null ? user.getId() : "Guest");
        
        return saved;
    }
    
    /**
     * Generate title từ câu hỏi đầu tiên (cắt 60 ký tự)
     */
    private String generateTitle(String query) {
        if (query == null || query.isBlank()) {
            return "Cuộc trò chuyện mới";
        }
        
        String cleaned = query.trim();
        if (cleaned.length() <= 60) {
            return cleaned;
        }
        
        return cleaned.substring(0, 57) + "...";
    }
    
    /**
     * Lưu USER message vào DB
     */
    private AiChatMessage saveUserMessage(Conversation conversation, String content) {
        AiChatMessage message = AiChatMessage.builder()
            .conversation(conversation)
            .role(MessageRole.USER)
            .content(content)
            .build();
        
        AiChatMessage saved = messageRepository.save(message);
        conversation.addMessage(saved);
        return saved;
    }
    
    /**
     * Lấy N messages gần nhất (context window)
     */
    private List<AiChatMessage> getRecentMessages(Long conversationId, int limit) {
        return messageRepository.findRecentMessagesByConversation(
            conversationId, 
            PageRequest.of(0, limit)
        );
    }
    
    /**
     * Enrich AI response với metadata
     */
    private void enrichResponse(AiChatResponse response, long responseTimeMs, Conversation conversation) {
        response.setTimestamp(LocalDateTime.now());
        response.setResponseTimeMs(responseTimeMs);
        
        // Calculate confidence từ sources (nếu có)
        if (response.getConfidence() == null && response.getSources() != null && !response.getSources().isEmpty()) {
            double avgScore = response.getSources().stream()
                .filter(s -> s != null && s.getScore() != null)
                .mapToDouble(SourceDocument::getScore)
                .average()
                .orElse(0.85);
            response.setConfidence(avgScore);
        }
        
        // Suggest ticket nếu confidence thấp
        if (response.getConfidence() != null && response.getConfidence() < similarityThreshold) {
            response.setSuggestTicket(true);
            response.setTicketCategory("Tư vấn học vụ");
            response.setTicketPriority("MEDIUM");
        }
    }
    
    /**
     * Lưu ASSISTANT message vào DB
     */
    private AiChatMessage saveAssistantMessage(Conversation conversation, AiChatResponse response) {
        String sourcesJson = null;
        
        if (response.getSources() != null && !response.getSources().isEmpty()) {
            try {
                sourcesJson = objectMapper.writeValueAsString(response.getSources());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize sources: {}", e.getMessage());
            }
        }
        
        AiChatMessage message = AiChatMessage.builder()
            .conversation(conversation)
            .role(MessageRole.ASSISTANT)
            .content(response.getAnswer())
            .sourcesJson(sourcesJson)
            .provider(response.getProvider())
            .confidence(response.getConfidence())
            .tokensUsed(response.getTokenUsed())
            .responseTimeMs(response.getResponseTimeMs())
            .build();
        
        AiChatMessage saved = messageRepository.save(message);
        conversation.addMessage(saved);
        return saved;
    }
    
    /**
     * Update conversation metadata
     */
    private void updateConversationMetadata(Conversation conversation) {
        conversation.setLastMessageAt(LocalDateTime.now());
        long count = messageRepository.countByConversationId(conversation.getId());
        conversation.setMessageCount((int) count);
        
        // Sum total tokens
        if (conversation.getTotalTokensUsed() == null) {
            conversation.setTotalTokensUsed(0);
        }
        
        conversationRepository.save(conversation);
    }
    
    /**
     * Lấy lịch sử conversation của user
     */
    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findActiveConversationsByUser(userId);
    }
    
    /**
     * Lấy chi tiết conversation theo sessionId
     */
    @Transactional(readOnly = true)
    public Conversation getConversationBySessionId(String sessionId) {
        return conversationRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Conversation", "sessionId", sessionId
            ));
    }
    
    /**
     * Lấy tất cả messages trong conversation
     */
    @Transactional(readOnly = true)
    public List<AiChatMessage> getConversationMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
    
    /**
     * Xóa conversation (soft delete)
     */
    @Transactional
    public void deleteConversation(String sessionId, User currentUser) {
        Conversation conversation = getConversationBySessionId(sessionId);
        
        // Validate ownership
        if (currentUser != null && conversation.getUser() != null) {
            if (!conversation.getUser().getId().equals(currentUser.getId())) {
                throw new ResourceNotFoundException(
                    "Conversation not found or access denied"
                );
            }
        }
        
        conversation.setStatus(ConversationStatus.ARCHIVED);
        conversationRepository.save(conversation);
        
        log.info("Archived conversation: {}", sessionId);
    }
    
    /**
     * Chuyển conversation thành Ticket
     */
    @Transactional
    public void convertToTicket(String sessionId, Long ticketId) {
        Conversation conversation = getConversationBySessionId(sessionId);
        conversation.setStatus(ConversationStatus.CONVERTED_TO_TICKET);
        conversation.setTicketId(ticketId);
        conversationRepository.save(conversation);
        
        log.info("Converted conversation {} to ticket {}", sessionId, ticketId);
    }

    /**
     * 🆕 PHASE 5: Submit feedback cho AI message
     */
    @Transactional
    public void submitFeedback(Long messageId, String feedbackTypeStr, User user) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để gửi phản hồi");
        }
        AiChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
        
        // Parse feedbackType
        FeedbackType feedbackType;
        try {
            feedbackType = FeedbackType.valueOf(feedbackTypeStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid feedback type: " + feedbackTypeStr);
        }
        
        // Create or update feedback
        AiMessageFeedback feedback = feedbackRepository.findByMessageIdAndUserId(messageId, user.getId())
                .orElse(AiMessageFeedback.builder()
                        .message(message)
                        .user(user)
                        .build());
        
        feedback.setFeedbackType(feedbackType);
        feedbackRepository.save(feedback);
        
        log.info("User {} submitted {} feedback for message {}", user.getId(), feedbackType, messageId);
    }
    
    /**
     * 🔥 PHASE 3: SSE Streaming Chat Response
     * Stream AI response theo từng chunk (giống ChatGPT)
     * 
     * @param request AiChatRequest
     * @param currentUser User đang đăng nhập
     * @return SseEmitter phát sự kiện theo thời gian thực
     */
    public SseEmitter chatStream(AiChatRequest request, User currentUser) {
        // Timeout 3 phút cho streaming
        SseEmitter emitter = new SseEmitter(180_000L);
        
        // Chạy async để không block request thread
        CompletableFuture.runAsync(() -> {
            try {
                // 1. Validate rate limit
                if (currentUser != null) {
                    validateRateLimit(currentUser);
                }
                
                // 2. Load/Create conversation
                Conversation conversation = getOrCreateConversation(
                    request.getSessionId(), 
                    currentUser, 
                    request.getQuery()
                );
                
                // 3. Save user message
                saveUserMessage(conversation, request.getQuery());
                
                // 4. Gửi event "thinking" để hiển thị loading indicator
                emitter.send(SseEmitter.event()
                    .name("thinking")
                    .data("{\"status\":\"processing\",\"message\":\"Đang suy nghĩ...\"}")
                );
                
                long startTime = System.currentTimeMillis();
                
                // 5. Gọi AI Engine (blocking call - sẽ cải thiện sau nếu AI service hỗ trợ streaming)
                AiChatResponse aiResponse = aiChatbotService.getChatResponse(
                    request.getQuery(),
                    conversation.getSessionId(),
                    request.getProvider()
                );
                
                // 6. Stream response theo chunks (simulate typing effect)
                String fullAnswer = aiResponse.getAnswer();
                streamTextInChunks(emitter, fullAnswer);
                
                // 7. Enrich metadata
                long responseTime = System.currentTimeMillis() - startTime;
                enrichResponse(aiResponse, responseTime, conversation);
                
                // 8. Gửi metadata cuối cùng (sources, suggestions, confidence)
                emitter.send(SseEmitter.event()
                    .name("metadata")
                    .data(objectMapper.writeValueAsString(new StreamMetadata(
                        aiResponse.getConfidence(),
                        aiResponse.getSources(),
                        aiResponse.getSuggestTicket() ? List.of(aiResponse.getTicketCategory()) : List.of(),
                        responseTime,
                        conversation.getSessionId()
                    )))
                );
                
                // 9. Save assistant message
                saveAssistantMessage(conversation, aiResponse);
                updateConversationMetadata(conversation);
                
                // 10. Complete stream
                emitter.send(SseEmitter.event()
                    .name("done")
                    .data("{\"status\":\"completed\"}")
                );
                emitter.complete();
                
                log.info("SSE Stream completed - SessionId: {}, Duration: {}ms", 
                    conversation.getSessionId(), responseTime);
                
            } catch (RateLimitExceededException | ResourceNotFoundException ex) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"" + ex.getMessage() + "\"}")
                    );
                } catch (IOException e) {
                    log.error("Failed to send error event", e);
                }
                emitter.completeWithError(ex);
                
            } catch (Exception ex) {
                log.error("SSE Stream error", ex);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"Có lỗi xảy ra khi xử lý yêu cầu\"}")
                    );
                } catch (IOException e) {
                    log.error("Failed to send error event", e);
                }
                emitter.completeWithError(ex);
            }
        }, executorService);
        
        // Handle client disconnect
        emitter.onCompletion(() -> log.debug("SSE Emitter completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE Emitter timeout");
            emitter.complete();
        });
        
        return emitter;
    }
    
    /**
     * Stream text theo chunks với typing effect
     */
    private void streamTextInChunks(SseEmitter emitter, String text) throws IOException {
        int chunkSize = 20; // Số ký tự mỗi chunk
        int delay = 30; // ms delay giữa các chunk (tạo hiệu ứng typing)
        
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            String chunk = text.substring(i, end);
            
            emitter.send(SseEmitter.event()
                .name("chunk")
                .data(objectMapper.writeValueAsString(new StreamChunk(chunk)))
            );
            
            // Sleep để tạo typing effect
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * DTO cho stream chunk
     */
    private record StreamChunk(String content) {}
    
    /**
     * DTO cho metadata cuối stream
     */
    private record StreamMetadata(
        Double confidence,
        List<SourceDocument> sources,
        List<String> suggestedTickets,
        Long responseTimeMs,
        String sessionId
    ) {}
}
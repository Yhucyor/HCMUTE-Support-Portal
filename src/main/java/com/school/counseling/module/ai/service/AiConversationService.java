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
import com.school.counseling.module.ai.repository.ConversationRepository;
import com.school.counseling.module.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    private final AiChatbotService aiChatbotService; // Delegate AI logic
    private final ObjectMapper objectMapper;
    
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
            validateRateLimit(currentUser.getId());
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
     * Validate rate limit (100 messages/user/day)
     */
    private void validateRateLimit(Long userId) {
        LocalDateTime todayStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        Long messageCount = messageRepository.countUserMessagesToday(userId, todayStart);
        
        if (messageCount >= maxMessagesPerDay) {
            log.warn("Rate limit exceeded for userId: {}, count: {}", userId, messageCount);
            throw new RateLimitExceededException(
                String.format("Bạn đã vượt quá giới hạn %d tin nhắn/ngày. Vui lòng thử lại vào ngày mai.", maxMessagesPerDay)
            );
        }
    }
    
    /**
     * Load conversation từ DB hoặc tạo mới
     */
    private Conversation getOrCreateConversation(String sessionId, User user, String firstQuery) {
        if (sessionId != null && !sessionId.isBlank()) {
            return conversationRepository.findBySessionId(sessionId)
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
        
        return messageRepository.save(message);
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
                .mapToDouble(SourceDocument::getScore)
                .average()
                .orElse(0.0);
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
        
        return messageRepository.save(message);
    }
    
    /**
     * Update conversation metadata
     */
    private void updateConversationMetadata(Conversation conversation) {
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setMessageCount(conversation.getMessages().size());
        
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
}

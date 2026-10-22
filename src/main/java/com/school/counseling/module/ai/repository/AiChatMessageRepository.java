package com.school.counseling.module.ai.repository;

import com.school.counseling.module.ai.entity.AiChatMessage;
import com.school.counseling.module.ai.entity.MessageRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository cho AI Chat Message
 */
@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    
    /**
     * Lấy tất cả messages trong conversation (sắp xếp theo thời gian)
     */
    List<AiChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Đếm số message trong conversation
     */
    long countByConversationId(Long conversationId);
    
    /**
     * Lấy N messages gần nhất của conversation (dùng cho context)
     */
    @Query("SELECT m FROM AiChatMessage m WHERE m.conversation.id = :convId ORDER BY m.createdAt DESC")
    List<AiChatMessage> findRecentMessagesByConversation(@Param("convId") Long conversationId, Pageable pageable);
    
    /**
     * Đếm số câu hỏi của user trong khoảng thời gian (chỉ đếm role USER, không tính câu trả lời của AI)
     */
    @Query("SELECT COUNT(m) FROM AiChatMessage m WHERE m.conversation.user.id = :userId AND m.role = com.school.counseling.module.ai.entity.MessageRole.USER AND m.createdAt > :since")
    Long countUserMessagesToday(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    /**
     * Tính trung bình confidence của AI responses
     */
    @Query("SELECT AVG(m.confidence) FROM AiChatMessage m WHERE m.role = 'ASSISTANT' AND m.createdAt BETWEEN :from AND :to")
    Double avgConfidenceByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    
    /**
     * Đếm số message theo provider
     */
    @Query("SELECT m.provider, COUNT(m) FROM AiChatMessage m WHERE m.role = 'ASSISTANT' AND m.createdAt BETWEEN :from AND :to GROUP BY m.provider")
    List<Object[]> countByProviderAndPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    
    /**
     * Lấy messages có confidence thấp (cần review)
     */
    @Query("SELECT m FROM AiChatMessage m WHERE m.role = 'ASSISTANT' AND m.confidence < :threshold ORDER BY m.createdAt DESC")
    List<AiChatMessage> findLowConfidenceMessages(@Param("threshold") Double threshold, Pageable pageable);
    
    /**
     * Đếm tổng token đã sử dụng trong khoảng thời gian
     */
    @Query("SELECT SUM(m.tokensUsed) FROM AiChatMessage m WHERE m.createdAt BETWEEN :from AND :to")
    Long sumTokensUsed(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    
    /**
     * Lấy messages bị flag (báo cáo)
     */
    List<AiChatMessage> findByIsFlaggedTrueOrderByCreatedAtDesc();
}

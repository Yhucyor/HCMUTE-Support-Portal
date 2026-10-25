package com.school.counseling.module.ai.repository;

import com.school.counseling.module.ai.entity.Conversation;
import com.school.counseling.module.ai.entity.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho AI Conversation
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    /**
     * Tìm conversation theo sessionId (UUID)
     */
    Optional<Conversation> findBySessionId(String sessionId);
    
    /**
     * Lấy danh sách conversation của user (phân trang)
     */
    Page<Conversation> findByUserIdOrderByLastMessageAtDesc(Long userId, Pageable pageable);
    
    /**
     * Lấy conversation đang ACTIVE của user
     */
    @Query("SELECT c FROM Conversation c WHERE c.user.id = :userId AND c.status = 'ACTIVE' ORDER BY c.lastMessageAt DESC")
    List<Conversation> findActiveConversationsByUser(@Param("userId") Long userId);
    
    /**
     * Đếm số conversation của user từ thời điểm X
     */
    Long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);
    
    /**
     * Lấy conversation theo status
     */
    Page<Conversation> findByStatusOrderByLastMessageAtDesc(ConversationStatus status, Pageable pageable);
    
    /**
     * Tìm conversation đã chuyển thành Ticket
     */
    Optional<Conversation> findByTicketId(Long ticketId);
    
    /**
     * Đếm tổng số conversation trong hệ thống
     */
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.createdAt BETWEEN :from AND :to")
    Long countByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    
    /**
     * Lấy Top conversation có nhiều message nhất
     */
    @Query("SELECT c FROM Conversation c ORDER BY c.messageCount DESC")
    List<Conversation> findTopByMessageCount(Pageable pageable);
}

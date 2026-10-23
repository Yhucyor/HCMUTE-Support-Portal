package com.school.counseling.module.ai.repository;

import com.school.counseling.module.ai.entity.AiMessageFeedback;
import com.school.counseling.module.ai.entity.FeedbackType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho AI Message Feedback
 */
@Repository
public interface AiMessageFeedbackRepository extends JpaRepository<AiMessageFeedback, Long> {
    
    /**
     * Lấy feedback của message
     */
    List<AiMessageFeedback> findByMessageIdOrderByCreatedAtDesc(Long messageId);
    
    /**
     * Kiểm tra user đã feedback message chưa
     */
    Optional<AiMessageFeedback> findByMessageIdAndUserId(Long messageId, Long userId);
    
    /**
     * Đếm số feedback theo loại
     */
    @Query("SELECT f.feedbackType, COUNT(f) FROM AiMessageFeedback f WHERE f.createdAt BETWEEN :from AND :to GROUP BY f.feedbackType")
    List<Object[]> countByTypeAndPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    
    /**
     * Tính tỷ lệ THUMBS_UP / THUMBS_DOWN
     */
    @Query("SELECT " +
           "  SUM(CASE WHEN f.feedbackType = 'THUMBS_UP' THEN 1 ELSE 0 END) * 100.0 / COUNT(*) " +
           "FROM AiMessageFeedback f " +
           "WHERE f.feedbackType IN ('THUMBS_UP', 'THUMBS_DOWN') AND f.createdAt BETWEEN :from AND :to")
    Double calculateSatisfactionRate(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    
    /**
     * Lấy feedback loại REPORT (cần xem xét)
     */
    List<AiMessageFeedback> findByFeedbackTypeOrderByCreatedAtDesc(FeedbackType feedbackType);
}

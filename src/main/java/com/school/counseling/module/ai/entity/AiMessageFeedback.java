package com.school.counseling.module.ai.entity;

import com.school.counseling.common.entity.BaseEntity;
import com.school.counseling.module.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entity cho feedback từ user về AI message
 * Dùng để cải thiện chất lượng AI qua thời gian
 */
@Entity
@Table(name = "ai_message_feedbacks", indexes = {
    @Index(name = "idx_feedback_msg", columnList = "message_id"),
    @Index(name = "idx_feedback_user", columnList = "user_id"),
    @Index(name = "idx_feedback_type", columnList = "feedback_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AiMessageFeedback extends BaseEntity {
    
    /**
     * Message được feedback
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private AiChatMessage message;
    
    /**
     * Loại feedback
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 30)
    private FeedbackType feedbackType;
    
    /**
     * Comment từ user (tùy chọn)
     */
    @Column(name = "comment", length = 1000)
    private String comment;
    
    /**
     * User gửi feedback (NULL nếu Guest)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    /**
     * Email Guest (nếu không đăng nhập)
     */
    @Column(name = "guest_email", length = 150)
    private String guestEmail;
    
    /**
     * Rating score (1-5 sao)
     */
    @Column(name = "rating")
    private Integer rating;
}

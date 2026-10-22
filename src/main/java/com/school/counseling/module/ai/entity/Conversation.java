package com.school.counseling.module.ai.entity;

import com.school.counseling.common.entity.BaseEntity;
import com.school.counseling.module.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity quản lý phiên hội thoại AI Chat
 * Mỗi conversation chứa nhiều messages (USER + ASSISTANT)
 */
@Entity
@Table(name = "ai_conversations", indexes = {
    @Index(name = "idx_conv_session_id", columnList = "session_id", unique = true),
    @Index(name = "idx_conv_user_last_msg", columnList = "user_id, last_message_at"),
    @Index(name = "idx_conv_status", columnList = "status")
})
@SQLDelete(sql = "UPDATE ai_conversations SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Conversation extends BaseEntity {
    
    /**
     * UUID định danh duy nhất cho conversation
     * Sử dụng UUID để tránh lộ thông tin internal ID
     */
    @Column(name = "session_id", nullable = false, unique = true, length = 36)
    private String sessionId;
    
    /**
     * Tiêu đề conversation (tự động sinh từ câu hỏi đầu tiên)
     */
    @Column(name = "title", length = 255)
    private String title;
    
    /**
     * User sở hữu conversation (NULL nếu Guest)
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    /**
     * Email của Guest (nếu không đăng nhập)
     */
    @Column(name = "guest_email", length = 150)
    private String guestEmail;
    
    /**
     * Trạng thái conversation
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ConversationStatus status;
    
    /**
     * Số lượng messages trong conversation
     */
    @Column(name = "message_count")
    private Integer messageCount;
    
    /**
     * Thời điểm message cuối cùng (dùng để sắp xếp)
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
    
    /**
     * Tổng số token đã sử dụng (tracking cost)
     */
    @Column(name = "total_tokens_used")
    private Integer totalTokensUsed;
    
    /**
     * Conversation có được chuyển thành Ticket không?
     */
    @Column(name = "ticket_id")
    private Long ticketId;
    
    /**
     * Danh sách messages trong conversation
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.Builder.Default
    private List<AiChatMessage> messages = new ArrayList<>();
    
    /**
     * Metadata dạng JSON (lưu thêm thông tin tùy chỉnh)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * Helper method: Thêm message vào conversation
     */
    public void addMessage(AiChatMessage message) {
        messages.add(message);
        message.setConversation(this);
        this.messageCount = messages.size();
        this.lastMessageAt = LocalDateTime.now();
    }
    
    /**
     * PrePersist: Khởi tạo giá trị mặc định
     */
    @PrePersist
    protected void onConversationCreate() {
        super.onCreate();
        if (this.status == null) {
            this.status = ConversationStatus.ACTIVE;
        }
        if (this.messageCount == null) {
            this.messageCount = 0;
        }
        if (this.totalTokensUsed == null) {
            this.totalTokensUsed = 0;
        }
    }
}

package com.school.counseling.module.ai.entity;

import com.school.counseling.common.entity.BaseEntity;
import com.school.counseling.module.auth.entity.Department;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "faqs")
@SQLDelete(sql = "UPDATE faqs SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Faq extends BaseEntity {

    @NotBlank(message = "Câu hỏi không được để trống")
    @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự")
    @Column(name = "question", length = 500, nullable = false)
    private String question;

    @NotBlank(message = "Câu trả lời không được để trống")
    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;

    @NotNull(message = "Đơn vị không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Size(max = 100, message = "Danh mục tối đa 100 ký tự")
    @Column(name = "category", length = 100)
    private String category;

    @Size(max = 255, message = "Từ khóa tối đa 255 ký tự")
    @Column(name = "keywords", length = 255)
    private String keywords;

    @Builder.Default
    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "post_date")
    private LocalDateTime postDate;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

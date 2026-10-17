package com.school.counseling.module.academic.entity;

import com.school.counseling.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "academic_sync_histories")
public class SyncHistory extends BaseEntity {

    @Column(name = "student_id", nullable = false, length = 20)
    private String studentId;

    @Column(name = "status", length = 30)
    private String status; // SUCCESS, FAILED, DEMO_FALLBACK

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "grades_count")
    private Integer gradesCount;

    @Column(name = "schedules_count")
    private Integer schedulesCount;

    @Column(name = "curriculums_count")
    private Integer curriculumsCount;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}

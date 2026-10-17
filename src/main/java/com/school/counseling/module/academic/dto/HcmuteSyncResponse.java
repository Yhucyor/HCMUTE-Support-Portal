package com.school.counseling.module.academic.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HcmuteSyncResponse {
    private boolean success;
    private String message;
    private String studentId;
    private String studentName;
    private int gradesCount;
    private int schedulesCount;
    private int curriculumsCount;
    private String syncMode; // "LIVE_PORTAL" or "DEMO_FALLBACK"
    private LocalDateTime syncedAt;
}

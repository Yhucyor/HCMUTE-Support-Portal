package com.school.counseling.module.academic.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicSummaryResponse {
    private String studentId;
    private String studentName;
    private int totalCredits;
    private Double gpa10;
    private Double gpa4;
    private int passedCoursesCount;
    private int failedCoursesCount;
    private String academicStanding;
    private String syncStatus;
    private LocalDateTime lastSyncedAt;

    // Các chỉ số nâng cao chuẩn quy chế HCMUTE 2025
    private Integer debtCredits;
    private String academicYearLevel;
    private Boolean isAcademicWarning;
    private String warningReason;
}

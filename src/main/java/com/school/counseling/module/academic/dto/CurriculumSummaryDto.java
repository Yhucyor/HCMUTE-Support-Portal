package com.school.counseling.module.academic.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurriculumSummaryDto {
    private String studentId;
    private String studentName;
    private String major;
    private String faculty;
    private String className;
    private String academicYear;
    private int totalRequiredCredits;
    private int completedCredits;
    private int inProgressCredits;
    private int remainingCredits;
    private double progressPercentage;
    private int completedCoursesCount;
    private int inProgressCoursesCount;
    private int remainingCoursesCount;
    private int totalCoursesCount;
    private List<CurriculumCourseDto> courses;
    private LocalDateTime lastSyncedAt;
    private String syncStatus;
}

package com.school.counseling.module.academic.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurriculumGroupDto {
    private String groupName;
    private Boolean isElective;
    private int requiredCredits;
    private int completedCredits;
    private int totalCreditsInGroup;
    private boolean isGroupCompleted;
    private double groupProgressPercentage;
    private int completedCoursesCount;
    private int totalCoursesCount;
    private List<CurriculumCourseDto> courses;
}

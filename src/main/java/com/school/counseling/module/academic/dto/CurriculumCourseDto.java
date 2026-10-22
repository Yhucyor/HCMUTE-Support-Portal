package com.school.counseling.module.academic.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurriculumCourseDto {
    private Long id;
    private String courseCode;
    private String courseName;
    private Integer credits;
    private Integer semesterRecommended;
    private String knowledgeBlock; // "Đại cương", "Cơ sở ngành", "Chuyên ngành", "Thực tập & Tốt nghiệp"
    private String status;         // "COMPLETED", "IN_PROGRESS", "FAILED", "NOT_TAKEN"
    private String statusLabel;    // "Đã đạt", "Đang học", "Chưa đạt", "Chưa học"
    private String gradeLetter;    // "A", "B+", "--", etc.
    private Double finalScore;     // thang 10
    private Double scale4Score;    // thang 4
    private String prerequisiteCode;
}

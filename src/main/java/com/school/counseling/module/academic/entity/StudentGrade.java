package com.school.counseling.module.academic.entity;

import com.school.counseling.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "academic_student_grades")
public class StudentGrade extends BaseEntity {

    @Column(name = "student_id", nullable = false, length = 50)
    private String studentId;

    @Column(name = "course_code", nullable = false, length = 50)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    @Column(name = "credits")
    private Integer credits;

    @Column(name = "midterm_score")
    private Double midtermScore;

    @Column(name = "final_score")
    private Double finalScore;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "grade_letter", length = 10)
    private String gradeLetter;

    @Column(name = "semester", length = 100)
    private String semester;

    @Column(name = "is_passed")
    private Boolean isPassed;
}

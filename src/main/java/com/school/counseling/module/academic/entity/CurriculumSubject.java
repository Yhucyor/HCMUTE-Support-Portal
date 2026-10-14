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
@Table(name = "academic_curriculum_subjects")
public class CurriculumSubject extends BaseEntity {

    @Column(name = "student_id", nullable = false, length = 20)
    private String studentId;

    @Column(name = "course_code", nullable = false, length = 30)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 150)
    private String courseName;

    @Column(name = "credits")
    private Integer credits;

    @Column(name = "is_completed")
    private Boolean isCompleted;

    @Column(name = "semester_recommended")
    private Integer semesterRecommended;
}

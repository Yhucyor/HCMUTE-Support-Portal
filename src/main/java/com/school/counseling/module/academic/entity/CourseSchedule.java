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
@Table(name = "academic_course_schedules")
public class CourseSchedule extends BaseEntity {

    @Column(name = "student_id", nullable = false, length = 50)
    private String studentId;

    @Column(name = "course_code", nullable = false, length = 50)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    @Column(name = "day_of_week", length = 30)
    private String dayOfWeek; // Thứ 2, Thứ 3...

    @Column(name = "shift", length = 50)
    private String shift; // Tiết 1-3, Tiết 7-9...

    @Column(name = "room", length = 100)
    private String room; // A1-302, E1-401...

    @Column(name = "lecturer", length = 150)
    private String lecturer;

    @Column(name = "semester", length = 100)
    private String semester;
}

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
@Table(name = "academic_student_profiles")
public class StudentProfile extends BaseEntity {

    @Column(name = "student_id", nullable = false, unique = true, length = 50)
    private String studentId;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "birthday", length = 30)
    private String birthday;

    @Column(name = "birth_place", length = 100)
    private String birthPlace;

    @Column(name = "student_class", length = 50)
    private String studentClass;

    @Column(name = "faculty", length = 150)
    private String faculty;

    @Column(name = "major", length = 150)
    private String major;

    @Column(name = "academic_year", length = 50)
    private String academicYear;

    @Column(name = "education_type", length = 50)
    private String educationType;

    @Column(name = "study_program", length = 150)
    private String studyProgram;

    @Column(name = "academic_status", length = 50)
    private String academicStatus;

    @Column(name = "academic_advisor", length = 150)
    private String academicAdvisor;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "citizen_id", length = 30)
    private String citizenId;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
}

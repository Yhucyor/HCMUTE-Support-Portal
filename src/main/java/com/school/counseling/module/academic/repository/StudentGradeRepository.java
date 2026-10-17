package com.school.counseling.module.academic.repository;

import com.school.counseling.module.academic.entity.StudentGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
    List<StudentGrade> findByStudentIdOrderBySemesterDesc(String studentId);
    void deleteByStudentId(String studentId);
}

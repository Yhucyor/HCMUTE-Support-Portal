package com.school.counseling.module.academic.repository;

import com.school.counseling.module.academic.entity.StudentGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
    List<StudentGrade> findByStudentIdOrderBySemesterDesc(String studentId);
    List<StudentGrade> findByStudentIdAndSemesterOrderByCourseNameAsc(String studentId, String semester);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT g.semester FROM StudentGrade g WHERE g.studentId = :studentId ORDER BY g.semester DESC")
    List<String> findDistinctSemestersByStudentId(@org.springframework.data.repository.query.Param("studentId") String studentId);

    void deleteByStudentId(String studentId);
}

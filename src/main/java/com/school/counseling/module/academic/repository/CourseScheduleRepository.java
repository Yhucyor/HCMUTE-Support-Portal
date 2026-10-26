package com.school.counseling.module.academic.repository;

import com.school.counseling.module.academic.entity.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {
    List<CourseSchedule> findByStudentIdOrderByDayOfWeekAsc(String studentId);
    List<CourseSchedule> findByStudentIdAndSemesterOrderByDayOfWeekAsc(String studentId, String semester);
    List<CourseSchedule> findByStudentIdAndSemesterAndPhaseOrderByDayOfWeekAsc(String studentId, String semester, String phase);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM CourseSchedule s WHERE s.studentId = :studentId")
    void deleteByStudentId(@org.springframework.data.repository.query.Param("studentId") String studentId);
}

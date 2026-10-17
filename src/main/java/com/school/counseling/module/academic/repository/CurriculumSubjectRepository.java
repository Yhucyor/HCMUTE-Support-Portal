package com.school.counseling.module.academic.repository;

import com.school.counseling.module.academic.entity.CurriculumSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurriculumSubjectRepository extends JpaRepository<CurriculumSubject, Long> {
    List<CurriculumSubject> findByStudentIdOrderBySemesterRecommendedAsc(String studentId);
    void deleteByStudentId(String studentId);
}

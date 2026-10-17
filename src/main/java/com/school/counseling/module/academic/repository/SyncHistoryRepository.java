package com.school.counseling.module.academic.repository;

import com.school.counseling.module.academic.entity.SyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncHistoryRepository extends JpaRepository<SyncHistory, Long> {
    List<SyncHistory> findByStudentIdOrderBySyncedAtDesc(String studentId);
}

package com.school.counseling.module.feed.repository;

import com.school.counseling.module.feed.entity.PostReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    @Query(value = "SELECT r FROM PostReport r " +
                   "LEFT JOIN FETCH r.post p " +
                   "LEFT JOIN FETCH p.author " +
                   "LEFT JOIN FETCH r.reporter " +
                   "WHERE (:status IS NULL OR :status = '' OR :status = 'ALL' OR r.status = :status) AND r.isDeleted = false " +
                   "ORDER BY r.createdAt DESC",
           countQuery = "SELECT count(r) FROM PostReport r WHERE (:status IS NULL OR :status = '' OR :status = 'ALL' OR r.status = :status) AND r.isDeleted = false")
    Page<PostReport> findReportsByStatus(@Param("status") String status, Pageable pageable);

    long countByStatus(String status);
}

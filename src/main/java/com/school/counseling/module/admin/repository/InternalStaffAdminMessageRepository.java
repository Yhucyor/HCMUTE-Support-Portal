package com.school.counseling.module.admin.repository;

import com.school.counseling.module.admin.entity.InternalStaffAdminMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternalStaffAdminMessageRepository extends JpaRepository<InternalStaffAdminMessage, Long> {

    @Query("""
        SELECT m FROM InternalStaffAdminMessage m
        LEFT JOIN FETCH m.sender s
        LEFT JOIN FETCH s.role
        LEFT JOIN FETCH s.department
        LEFT JOIN FETCH m.receiver r
        LEFT JOIN FETCH m.department d
        WHERE m.isDeleted = false
          AND (:deptId IS NULL OR m.department.id = :deptId)
        ORDER BY m.createdAt DESC
    """)
    List<InternalStaffAdminMessage> findRecentMessagesByDepartment(@Param("deptId") Long deptId);

    @Query("""
        SELECT m FROM InternalStaffAdminMessage m
        LEFT JOIN FETCH m.sender s
        LEFT JOIN FETCH s.role
        LEFT JOIN FETCH s.department
        LEFT JOIN FETCH m.receiver r
        LEFT JOIN FETCH m.department d
        WHERE m.isDeleted = false
        ORDER BY m.createdAt DESC
    """)
    List<InternalStaffAdminMessage> findAllRecentMessages();

    @Query("""
        SELECT COUNT(m) FROM InternalStaffAdminMessage m
        WHERE m.isDeleted = false
          AND m.isRead = false
          AND (:deptId IS NULL OR m.department.id = :deptId)
    """)
    long countUnreadMessages(@Param("deptId") Long deptId);
}

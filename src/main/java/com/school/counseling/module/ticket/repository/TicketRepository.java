package com.school.counseling.module.ticket.repository;

import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.ticket.dto.TicketAccessAuthInfo;
import com.school.counseling.module.ticket.dto.TicketSummaryDto;
import com.school.counseling.module.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // ─── Truy vấn JOIN FETCH đầy đủ để tránh LazyInitializationException khi OSIV = false ───

    /**
     * Tải Ticket theo ID kèm JOIN FETCH department, creator, assignedTo.
     * BẮT BUỘC dùng thay thế findById khi cần trả ra View hoặc mapToDto.
     */
    @Query("""
        SELECT t FROM Ticket t
        LEFT JOIN FETCH t.department
        LEFT JOIN FETCH t.creator
        LEFT JOIN FETCH t.assignedTo
        WHERE t.id = :id AND t.isDeleted = false
    """)
    Optional<Ticket> findByIdWithRelations(@Param("id") Long id);

    @Query("""
        SELECT t FROM Ticket t
        LEFT JOIN FETCH t.department
        LEFT JOIN FETCH t.creator
        LEFT JOIN FETCH t.assignedTo
        WHERE t.ticketCode = :code AND t.isDeleted = false
    """)
    Optional<Ticket> findByTicketCodeWithRelations(@Param("code") String ticketCode);

    @Query("""
        SELECT t FROM Ticket t
        LEFT JOIN FETCH t.department
        LEFT JOIN FETCH t.creator
        LEFT JOIN FETCH t.assignedTo
        WHERE t.guestToken = :token AND t.isDeleted = false
    """)
    Optional<Ticket> findByGuestTokenWithRelations(@Param("token") String guestToken);

    // ─── DTO Projection cho danh sách: chỉ lấy các trường cần thiết, tránh N+1 ───

    /**
     * TicketSummaryDto Projection: Lấy đúng 10 trường cần thiết cho Dashboard (không tải history/attachment).
     * Tốc độ tăng gấp 5-10x so với mapToDto toàn bộ entity.
     */
    @Query("""
        SELECT new com.school.counseling.module.ticket.dto.TicketSummaryDto(
            t.id, t.ticketCode, t.title,
            d.id, d.name,
            t.status, t.priority, t.dueDate, t.createdAt,
            COALESCE(c.fullName, t.guestName),
            a.fullName,
            COALESCE(t.hasStudentReply, false),
            t.rating
        )
        FROM Ticket t
        JOIN t.department d
        LEFT JOIN t.creator c
        LEFT JOIN t.assignedTo a
        WHERE d.id = :deptId AND t.isDeleted = false
        ORDER BY t.createdAt DESC
    """)
    List<TicketSummaryDto> findSummaryByDepartmentId(@Param("deptId") Long departmentId);

    @Query("""
        SELECT new com.school.counseling.module.ticket.dto.TicketSummaryDto(
            t.id, t.ticketCode, t.title,
            d.id, d.name,
            t.status, t.priority, t.dueDate, t.createdAt,
            COALESCE(c.fullName, t.guestName),
            a.fullName,
            COALESCE(t.hasStudentReply, false),
            t.rating
        )
        FROM Ticket t
        JOIN t.department d
        LEFT JOIN t.creator c
        LEFT JOIN t.assignedTo a
        WHERE d.id = :deptId AND t.status = :status AND t.isDeleted = false
        ORDER BY t.createdAt DESC
    """)
    List<TicketSummaryDto> findSummaryByDepartmentIdAndStatus(@Param("deptId") Long departmentId,
                                                               @Param("status") String status);

    @Query("""
        SELECT new com.school.counseling.module.ticket.dto.TicketSummaryDto(
            t.id, t.ticketCode, t.title,
            d.id, d.name,
            t.status, t.priority, t.dueDate, t.createdAt,
            COALESCE(c.fullName, t.guestName),
            a.fullName,
            COALESCE(t.hasStudentReply, false),
            t.rating
        )
        FROM Ticket t
        JOIN t.department d
        LEFT JOIN t.creator c
        LEFT JOIN t.assignedTo a
        WHERE c.id = :userId AND t.isDeleted = false
        ORDER BY t.createdAt DESC
    """)
    List<TicketSummaryDto> findSummaryByCreatorId(@Param("userId") Long userId);

    @Query("""
        SELECT new com.school.counseling.module.ticket.dto.TicketSummaryDto(
            t.id, t.ticketCode, t.title,
            d.id, d.name,
            t.status, t.priority, t.dueDate, t.createdAt,
            COALESCE(c.fullName, t.guestName),
            a.fullName,
            COALESCE(t.hasStudentReply, false),
            t.rating
        )
        FROM Ticket t
        JOIN t.department d
        LEFT JOIN t.creator c
        LEFT JOIN t.assignedTo a
        WHERE t.isDeleted = false
        ORDER BY t.createdAt DESC
    """)
    List<TicketSummaryDto> findAllSummaries();

    @Query("""
        SELECT new com.school.counseling.module.ticket.dto.TicketSummaryDto(
            t.id, t.ticketCode, t.title,
            d.id, d.name,
            t.status, t.priority, t.dueDate, t.createdAt,
            COALESCE(c.fullName, t.guestName),
            a.fullName,
            COALESCE(t.hasStudentReply, false),
            t.rating
        )
        FROM Ticket t
        JOIN t.department d
        LEFT JOIN t.creator c
        LEFT JOIN t.assignedTo a
        WHERE t.status = :status AND t.isDeleted = false
        ORDER BY t.createdAt DESC
    """)
    List<TicketSummaryDto> findAllSummariesByStatus(@Param("status") String status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.department.id = :deptId AND t.hasStudentReply = true AND t.isDeleted = false")
    long countByDepartmentIdAndHasStudentReplyTrue(@Param("deptId") Long deptId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.hasStudentReply = true AND t.isDeleted = false")
    long countAllByHasStudentReplyTrue();

    // ─── Atomic Update: Chống Race Condition khi nhiều Cán bộ cùng nhận Ticket ───

    /**
     * Cập nhật nguyên tử (Atomic) tại MySQL Row-Level X-Lock:
     * - Chỉ cập nhật khi status = 'OPEN' VÀ assignedTo IS NULL
     * - Trả về số dòng bị ảnh hưởng:
     *   + 1 → Thành công, Cán bộ này nhận được vé
     *   + 0 → Đã có Cán bộ khác nhận trước đó → ném TicketAlreadyClaimedException
     *
     * clearAutomatically = true: Xóa Hibernate First-Level Cache sau UPDATE để tránh đọc dữ liệu cũ (stale)
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Ticket t
        SET t.assignedTo = :staff, t.status = 'IN_PROGRESS'
        WHERE t.id = :ticketId AND t.status = 'OPEN' AND t.assignedTo IS NULL
    """)
    int claimTicketAtomic(@Param("ticketId") Long ticketId, @Param("staff") User staff);

    // ─── Truy vấn SLA Engine ───

    @Query("SELECT t FROM Ticket t WHERE t.status IN ('OPEN', 'IN_PROGRESS') AND t.dueDate BETWEEN :now AND :threshold AND t.isDeleted = false")
    List<Ticket> findDueSoonTickets(@Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);

    @Query("SELECT t FROM Ticket t WHERE t.status IN ('OPEN', 'IN_PROGRESS') AND t.dueDate < :now AND t.isDeleted = false")
    List<Ticket> findOverdueTickets(@Param("now") LocalDateTime now);

    @Query("""
        SELECT t FROM Ticket t
        LEFT JOIN FETCH t.department
        LEFT JOIN FETCH t.assignedTo
        LEFT JOIN FETCH t.creator
        WHERE t.status IN ('OPEN', 'IN_PROGRESS') AND t.dueDate < :now AND t.isDeleted = false
    """)
    List<Ticket> findOverdueTicketsWithRelations(@Param("now") LocalDateTime now);

    @Query("""
        SELECT t FROM Ticket t
        LEFT JOIN FETCH t.department
        LEFT JOIN FETCH t.creator
        LEFT JOIN FETCH t.assignedTo
        WHERE t.status = 'RESOLVED'
          AND (t.resolvedAt <= :threshold OR (t.resolvedAt IS NULL AND t.updatedAt <= :threshold))
          AND t.isDeleted = false
    """)
    List<Ticket> findResolvedTicketsEligibleForAutoClose(@Param("threshold") LocalDateTime threshold);

    // ─── Phân quyền: Tải thông tin sở hữu tối thiểu để kiểm tra quyền (không load toàn Entity) ───

    @Query("""
        SELECT t.id AS id,
               t.creator.id AS creatorId,
               t.department.id AS departmentId
        FROM Ticket t
        WHERE t.id = :ticketId AND t.isDeleted = false
    """)
    Optional<TicketAccessAuthInfo> findAccessAuthInfoById(@Param("ticketId") Long ticketId);

    // ─── SLA Analytics & Dashboard Queries ───

    long countByDepartmentId(Long departmentId);

    @Query("""
        SELECT COUNT(t) FROM Ticket t
        WHERE t.isDeleted = false
          AND (:since IS NULL OR t.createdAt >= :since)
          AND (:deptId IS NULL OR t.department.id = :deptId)
    """)
    long countTicketsFiltered(@Param("since") LocalDateTime since, @Param("deptId") Long deptId);

    @Query("""
        SELECT COUNT(t) FROM Ticket t
        WHERE t.isDeleted = false
          AND t.status = :status
          AND (:since IS NULL OR t.createdAt >= :since)
          AND (:deptId IS NULL OR t.department.id = :deptId)
    """)
    long countTicketsByStatusFiltered(@Param("status") String status,
                                      @Param("since") LocalDateTime since,
                                      @Param("deptId") Long deptId);

    @Query("""
        SELECT COUNT(t) FROM Ticket t
        WHERE t.isDeleted = false
          AND (t.status = 'OVERDUE' OR (t.status IN ('OPEN', 'IN_PROGRESS') AND t.dueDate < CURRENT_TIMESTAMP))
          AND (:since IS NULL OR t.createdAt >= :since)
          AND (:deptId IS NULL OR t.department.id = :deptId)
    """)
    long countOverdueTicketsFiltered(@Param("since") LocalDateTime since, @Param("deptId") Long deptId);

    @Query("""
        SELECT COUNT(t) FROM Ticket t
        WHERE t.isDeleted = false
          AND t.status IN ('RESOLVED', 'CLOSED')
          AND (t.resolvedAt IS NOT NULL AND t.resolvedAt <= t.dueDate)
          AND (:since IS NULL OR t.createdAt >= :since)
          AND (:deptId IS NULL OR t.department.id = :deptId)
    """)
    long countResolvedOnTimeFiltered(@Param("since") LocalDateTime since, @Param("deptId") Long deptId);

    @Query("""
        SELECT AVG(t.rating) FROM Ticket t
        WHERE t.isDeleted = false
          AND t.rating IS NOT NULL
          AND (:since IS NULL OR t.createdAt >= :since)
          AND (:deptId IS NULL OR t.department.id = :deptId)
    """)
    Double getAverageRatingFiltered(@Param("since") LocalDateTime since, @Param("deptId") Long deptId);

    @Query("""
        SELECT t.priority, COUNT(t) FROM Ticket t
        WHERE t.isDeleted = false
          AND (:since IS NULL OR t.createdAt >= :since)
          AND (:deptId IS NULL OR t.department.id = :deptId)
        GROUP BY t.priority
    """)
    List<Object[]> countByPriorityFiltered(@Param("since") LocalDateTime since, @Param("deptId") Long deptId);

    @Query("""
        SELECT t.department.id,
               d.name,
               d.code,
               COUNT(t),
               SUM(CASE WHEN t.status = 'OPEN' THEN 1 ELSE 0 END),
               SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END),
               SUM(CASE WHEN t.status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END),
               SUM(CASE WHEN t.status = 'OVERDUE' OR (t.status IN ('OPEN', 'IN_PROGRESS') AND t.dueDate < CURRENT_TIMESTAMP) THEN 1 ELSE 0 END),
               AVG(CASE WHEN t.rating IS NOT NULL THEN t.rating ELSE NULL END)
        FROM Ticket t
        JOIN t.department d
        WHERE t.isDeleted = false
          AND (:since IS NULL OR t.createdAt >= :since)
        GROUP BY t.department.id, d.name, d.code
        ORDER BY COUNT(t) DESC
    """)
    List<Object[]> getDepartmentSlaAggregates(@Param("since") LocalDateTime since);

    @Query("""
        SELECT new com.school.counseling.module.ticket.dto.TicketSummaryDto(
            t.id, t.ticketCode, t.title,
            d.id, d.name,
            t.status, t.priority, t.dueDate, t.createdAt,
            COALESCE(c.fullName, t.guestName),
            a.fullName,
            COALESCE(t.hasStudentReply, false),
            t.rating
        )
        FROM Ticket t
        JOIN t.department d
        LEFT JOIN t.creator c
        LEFT JOIN t.assignedTo a
        WHERE t.isDeleted = false
          AND (t.status = 'OVERDUE' OR (t.status IN ('OPEN', 'IN_PROGRESS') AND t.dueDate < CURRENT_TIMESTAMP))
          AND (:deptId IS NULL OR d.id = :deptId)
        ORDER BY t.dueDate ASC
    """)
    List<TicketSummaryDto> findTopOverdueTicketsFiltered(@Param("deptId") Long deptId);
}

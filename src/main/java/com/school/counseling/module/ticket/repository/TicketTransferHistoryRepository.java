package com.school.counseling.module.ticket.repository;

import com.school.counseling.module.ticket.entity.TicketTransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketTransferHistoryRepository extends JpaRepository<TicketTransferHistory, Long> {

    @Query("""
        SELECT tth FROM TicketTransferHistory tth
        LEFT JOIN FETCH tth.fromDepartment
        LEFT JOIN FETCH tth.toDepartment
        LEFT JOIN FETCH tth.transferredBy
        WHERE tth.ticket.id = :ticketId
        ORDER BY tth.createdAt DESC
    """)
    List<TicketTransferHistory> findByTicketIdWithRelations(@Param("ticketId") Long ticketId);
}

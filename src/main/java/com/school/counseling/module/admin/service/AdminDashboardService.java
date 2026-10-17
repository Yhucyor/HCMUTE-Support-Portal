package com.school.counseling.module.admin.service;

import com.school.counseling.module.admin.dto.AdminSlaDashboardDto;
import com.school.counseling.module.admin.dto.DepartmentSlaStatDto;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.ticket.dto.TicketSummaryDto;
import com.school.counseling.module.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final TicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public AdminSlaDashboardDto getSlaDashboardStats(String timeRange, Long departmentId) {
        LocalDateTime since = null;
        if ("7_DAYS".equalsIgnoreCase(timeRange)) {
            since = LocalDateTime.now().minusDays(7);
        } else if ("30_DAYS".equalsIgnoreCase(timeRange)) {
            since = LocalDateTime.now().minusDays(30);
        } else {
            timeRange = "ALL";
        }

        long total = ticketRepository.countTicketsFiltered(since, departmentId);
        long open = ticketRepository.countTicketsByStatusFiltered("OPEN", since, departmentId);
        long inProgress = ticketRepository.countTicketsByStatusFiltered("IN_PROGRESS", since, departmentId);
        long resolved = ticketRepository.countTicketsByStatusFiltered("RESOLVED", since, departmentId);
        long closed = ticketRepository.countTicketsByStatusFiltered("CLOSED", since, departmentId);
        long overdue = ticketRepository.countOverdueTicketsFiltered(since, departmentId);
        long totalResolved = resolved + closed;
        long onTime = ticketRepository.countResolvedOnTimeFiltered(since, departmentId);

        double complianceRate = 100.0;
        if (totalResolved > 0) {
            complianceRate = Math.round(((double) onTime / totalResolved * 100.0) * 10.0) / 10.0;
        } else if (total > 0 && overdue > 0) {
            complianceRate = Math.round(((double) (total - overdue) / total * 100.0) * 10.0) / 10.0;
        }

        Double avgRating = ticketRepository.getAverageRatingFiltered(since, departmentId);
        if (avgRating != null) {
            avgRating = Math.round(avgRating * 10.0) / 10.0;
        }

        // Priority breakdown
        Map<String, Long> priorityStats = new LinkedHashMap<>();
        priorityStats.put("URGENT", 0L);
        priorityStats.put("HIGH", 0L);
        priorityStats.put("MEDIUM", 0L);
        priorityStats.put("LOW", 0L);

        List<Object[]> priorityRows = ticketRepository.countByPriorityFiltered(since, departmentId);
        for (Object[] row : priorityRows) {
            if (row != null && row.length >= 2 && row[0] != null) {
                String p = (String) row[0];
                long c = ((Number) row[1]).longValue();
                priorityStats.put(p.toUpperCase(), c);
            }
        }

        // Status breakdown
        Map<String, Long> statusStats = new LinkedHashMap<>();
        statusStats.put("OPEN", open);
        statusStats.put("IN_PROGRESS", inProgress);
        statusStats.put("RESOLVED", resolved);
        statusStats.put("CLOSED", closed);
        statusStats.put("OVERDUE", overdue);

        // Department SLA Performance Matrix
        List<Department> allDepts = departmentRepository.findAll();
        Map<Long, Object[]> aggMap = new HashMap<>();
        List<Object[]> aggRows = ticketRepository.getDepartmentSlaAggregates(since);
        for (Object[] row : aggRows) {
            if (row != null && row.length >= 1 && row[0] != null) {
                Long dId = ((Number) row[0]).longValue();
                aggMap.put(dId, row);
            }
        }

        List<DepartmentSlaStatDto> deptStats = new ArrayList<>();
        for (Department d : allDepts) {
            long staffCount = userRepository.countByDepartmentId(d.getId());
            Object[] row = aggMap.get(d.getId());
            if (row != null) {
                long dTotal = ((Number) row[3]).longValue();
                long dOpen = ((Number) row[4]).longValue();
                long dInProgress = ((Number) row[5]).longValue();
                long dResolved = ((Number) row[6]).longValue();
                long dOverdue = ((Number) row[7]).longValue();
                Double dRating = row[8] != null ? ((Number) row[8]).doubleValue() : null;
                if (dRating != null) {
                    dRating = Math.round(dRating * 10.0) / 10.0;
                }

                double dCompliance = 100.0;
                if (dTotal > 0) {
                    long dNonOverdue = Math.max(0, dTotal - dOverdue);
                    dCompliance = Math.round(((double) dNonOverdue / dTotal * 100.0) * 10.0) / 10.0;
                }

                deptStats.add(DepartmentSlaStatDto.builder()
                        .departmentId(d.getId())
                        .departmentName(d.getName())
                        .departmentCode(d.getCode())
                        .totalTickets(dTotal)
                        .openTickets(dOpen)
                        .inProgressTickets(dInProgress)
                        .resolvedTickets(dResolved)
                        .overdueTickets(dOverdue)
                        .slaComplianceRate(dCompliance)
                        .averageRating(dRating)
                        .staffCount(staffCount)
                        .build());
            } else {
                deptStats.add(DepartmentSlaStatDto.builder()
                        .departmentId(d.getId())
                        .departmentName(d.getName())
                        .departmentCode(d.getCode())
                        .totalTickets(0L)
                        .openTickets(0L)
                        .inProgressTickets(0L)
                        .resolvedTickets(0L)
                        .overdueTickets(0L)
                        .slaComplianceRate(100.0)
                        .averageRating(null)
                        .staffCount(staffCount)
                        .build());
            }
        }

        deptStats.sort((a, b) -> Long.compare(b.getTotalTickets(), a.getTotalTickets()));

        List<TicketSummaryDto> overdueList = ticketRepository.findTopOverdueTicketsFiltered(departmentId);

        String selectedDeptName = null;
        if (departmentId != null) {
            selectedDeptName = departmentRepository.findById(departmentId)
                    .map(Department::getName)
                    .orElse(null);
        }

        return AdminSlaDashboardDto.builder()
                .totalTickets(total)
                .openTickets(open)
                .inProgressTickets(inProgress)
                .resolvedTickets(resolved)
                .closedTickets(closed)
                .overdueTickets(overdue)
                .slaComplianceRate(complianceRate)
                .averageRating(avgRating)
                .totalResolved(totalResolved)
                .totalOnTime(onTime)
                .priorityStats(priorityStats)
                .statusStats(statusStats)
                .departmentStats(deptStats)
                .overdueTicketsList(overdueList)
                .timeRange(timeRange)
                .selectedDepartmentId(departmentId)
                .selectedDepartmentName(selectedDeptName)
                .build();
    }
}

package com.school.counseling.module.admin.dto;

import com.school.counseling.module.ticket.dto.TicketSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSlaDashboardDto {
    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long closedTickets;
    private long overdueTickets;
    private double slaComplianceRate;
    private Double averageRating;
    private long totalResolved;
    private long totalOnTime;

    private Map<String, Long> priorityStats;
    private Map<String, Long> statusStats;
    private List<DepartmentSlaStatDto> departmentStats;
    private List<TicketSummaryDto> overdueTicketsList;

    private String timeRange;
    private Long selectedDepartmentId;
    private String selectedDepartmentName;
}

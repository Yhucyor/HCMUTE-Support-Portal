package com.school.counseling.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSlaStatDto {
    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long overdueTickets;
    private double slaComplianceRate;
    private Double averageRating;
    private long staffCount;
}

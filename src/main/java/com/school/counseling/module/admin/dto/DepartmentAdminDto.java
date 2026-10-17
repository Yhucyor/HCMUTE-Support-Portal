package com.school.counseling.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentAdminDto {
    private Long id;
    private String name;
    private String code;
    private String officeLocation;
    private String contactEmail;
    private String contactPhone;
    private String description;
    private Boolean isActive;
    private long staffCount;
    private long totalTickets;
    private LocalDateTime createdAt;
}

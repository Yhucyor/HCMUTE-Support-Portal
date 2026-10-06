package com.school.counseling.module.auth.dto;

import com.school.counseling.module.auth.entity.Department;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDto {
    private Long id;
    private String name;
    private String code;
    private String officeLocation;
    private String contactEmail;
    private String contactPhone;
    private String description;
    private Boolean isActive;

    public static DepartmentDto fromEntity(Department department) {
        if (department == null) return null;
        return DepartmentDto.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .officeLocation(department.getOfficeLocation())
                .contactEmail(department.getContactEmail())
                .contactPhone(department.getContactPhone())
                .description(department.getDescription())
                .isActive(department.getIsActive())
                .build();
    }
}

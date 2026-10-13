package com.school.counseling.module.ticket.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTriageResponseDto {

    private Long suggestedDepartmentId;
    private String departmentCode;
    private String departmentName;
    private String priority;
    private String sentiment;
    private Double confidenceScore;
    private String summary;
    private String suggestedAction;
    private List<String> keyIndicators;
    private String classifiedBy;
}
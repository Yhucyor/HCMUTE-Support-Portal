package com.school.counseling.module.academic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalAnnouncementDto {
    private String id;
    private String title;
    private String contentHtml;
    private String category; // "Học vụ", "Học bổng", "Lịch thi", "Công tác SV", "Thông báo chung"
    private String publishDate;
    private String sender;
    private Boolean isPinned;
    private String type; // "CHUNG" hoặc "CA_NHAN"
    private Boolean isRead;
}

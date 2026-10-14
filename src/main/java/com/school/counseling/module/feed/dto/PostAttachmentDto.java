package com.school.counseling.module.feed.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostAttachmentDto {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String sourceType;
    private LocalDateTime createdAt;
}

package com.school.counseling.module.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeToggleResponseDto {
    private String targetType;
    private Long targetId;
    private boolean liked;
    private int likeCount;
    private String message;
}

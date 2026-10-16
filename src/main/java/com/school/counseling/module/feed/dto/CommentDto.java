package com.school.counseling.module.feed.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {

    private Long id;
    private Long postId;
    private Long authorId;
    private String authorName;
    private String authorUsername;
    private String authorRole;
    private String authorDepartment;
    private String authorAvatar;
    private String content;
    private Long parentId;
    private Integer likeCount;
    private boolean userLiked;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<CommentDto> replies = new ArrayList<>();
}

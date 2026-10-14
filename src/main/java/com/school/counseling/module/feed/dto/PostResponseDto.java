package com.school.counseling.module.feed.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDto {

    private Long id;
    private String title;
    private String content;
    private String postType;
    private String status;
    private String videoStatus;
    private String videoUrl;
    private Boolean isPinned;
    private Integer viewCount;

    private Long departmentId;
    private String departmentName;

    private Long authorId;
    private String authorName;
    private String authorEmail;
    private String authorUsername;
    private String authorRole;
    private String authorAvatar;

    public record AuthorSummary(Long id, String fullName, String email, String username) {}

    public AuthorSummary getAuthor() {
        if (authorId == null && authorName == null) {
            return null;
        }
        return new AuthorSummary(authorId, authorName != null ? authorName : "Sinh viên", authorEmail, authorUsername);
    }

    public String getAuthorFullName() {
        return authorName != null ? authorName : "Sinh viên";
    }

    private Integer likeCount;
    private Integer commentCount;
    private boolean userLiked;
    private String rejectionReason;

    private List<PostAttachmentDto> attachments;
    private List<CommentDto> comments;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isYoutubeVideo() {
        if (videoUrl == null) return false;
        String lower = videoUrl.toLowerCase().trim();
        return lower.contains("youtube.com") || lower.contains("youtu.be");
    }

    public boolean getYoutubeVideo() {
        return isYoutubeVideo();
    }

    public String getYoutubeEmbedUrl() {
        if (!isYoutubeVideo()) return videoUrl;
        try {
            String url = videoUrl.trim();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(?:https?://)?(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?.*?v=|embed/|v/|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return "https://www.youtube.com/embed/" + matcher.group(1);
            }
            if (url.contains("/embed/")) {
                return url;
            }
        } catch (Exception ignored) {}
        return videoUrl;
    }

    public boolean isDriveVideo() {
        if (videoUrl == null) return false;
        return videoUrl.toLowerCase().contains("drive.google.com");
    }

    public boolean getDriveVideo() {
        return isDriveVideo();
    }

    public String getDriveEmbedUrl() {
        if (!isDriveVideo()) return videoUrl;
        if (videoUrl.contains("/view")) {
            return videoUrl.replace("/view", "/preview");
        }
        return videoUrl;
    }

    public boolean isDirectVideo() {
        if (videoUrl == null || videoUrl.isBlank()) return false;
        return !isYoutubeVideo() && !isDriveVideo();
    }

    public boolean getDirectVideo() {
        return isDirectVideo();
    }
}

package com.school.counseling.module.feed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePostRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private Long departmentId;

    @Builder.Default
    private String postType = "OFFICIAL_ANNOUNCEMENT";

    private boolean requestVideo;

    @Builder.Default
    private Boolean isPinned = false;

    public Boolean isPinned() {
        return Boolean.TRUE.equals(isPinned);
    }

    // Video thông báo (Link nhúng YouTube/Drive hoặc URL file nội bộ)
    private String videoEmbedUrl;
    private String videoUrl;
    private String youtubeUrl;

    public String getVideoEmbedUrl() {
        if (videoEmbedUrl != null && !videoEmbedUrl.isBlank()) return videoEmbedUrl;
        return youtubeUrl;
    }

    public String getYoutubeUrl() {
        if (youtubeUrl != null && !youtubeUrl.isBlank()) return youtubeUrl;
        return videoEmbedUrl;
    }

    // File đính kèm tài liệu (PDF, Word, TXT)
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
}

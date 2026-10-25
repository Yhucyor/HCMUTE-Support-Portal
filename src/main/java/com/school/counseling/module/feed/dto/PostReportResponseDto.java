package com.school.counseling.module.feed.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostReportResponseDto {
    private Long id;
    private PostSummary post;
    private ReporterSummary reporter;
    private String reason;
    private String details;
    private String status;
    private String actionTaken;
    private String resolvedByName;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public record PostSummary(Long id, String title, String content, String status, AuthorSummary author) {
        public PostSummary(Long id, String title, String content, AuthorSummary author) {
            this(id, title, content, "APPROVED", author);
        }

        public String getPlainTextSummary() {
            if (content == null || content.isBlank()) return "";
            String plain = content.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ").trim();
            return plain.length() > 120 ? plain.substring(0, 120) + "..." : plain;
        }

        public String plainTextSummary() {
            return getPlainTextSummary();
        }
    }
    public record AuthorSummary(Long id, String fullName, String email) {}
    public record ReporterSummary(Long id, String fullName, String email) {}
}

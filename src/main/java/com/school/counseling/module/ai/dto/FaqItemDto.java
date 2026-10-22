package com.school.counseling.module.ai.dto;

import com.school.counseling.module.ai.entity.Faq;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Record DTO đại diện cho câu hỏi FAQ trả về cho View và REST API.
 * Thay thế hoàn toàn việc truyền trực tiếp JPA Entity Faq ra ngoài.
 */
public record FaqItemDto(
        Long id,
        String question,
        String answer,
        Long departmentId,
        String departmentName,
        String category,
        String categorySlug,
        Integer viewCount,
        String formattedDate
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static FaqItemDto fromEntity(Faq faq) {
        if (faq == null) return null;

        String deptName = (faq.getDepartment() != null) ? faq.getDepartment().getName() : "Phòng Đào tạo & CTSV";
        Long deptId = (faq.getDepartment() != null) ? faq.getDepartment().getId() : 0L;
        String rawCat = (faq.getCategory() != null && !faq.getCategory().isBlank()) ? faq.getCategory() : "Học vụ & đào tạo";
        String slug = resolveCategorySlug(rawCat);

        LocalDateTime date = (faq.getPostDate() != null) ? faq.getPostDate() : faq.getCreatedAt();
        String dateStr = (date != null) ? date.format(FORMATTER) : "Mới cập nhật";

        return new FaqItemDto(
                faq.getId(),
                faq.getQuestion(),
                faq.getAnswer(),
                deptId,
                deptName,
                rawCat,
                slug,
                faq.getViewCount() != null ? faq.getViewCount() : 0,
                dateStr
        );
    }

    public static String resolveCategorySlug(String category) {
        if (category == null) return "hoc-vu";
        String lower = category.toLowerCase();
        if (lower.contains("học phí") || lower.contains("học bổng")) return "hoc-phi";
        if (lower.contains("ngoại ngữ") || lower.contains("tiếng anh") || lower.contains("toeic")) return "ngoai-ngu";
        if (lower.contains("tốt nghiệp") || lower.contains("quy chế")) return "quy-che";
        if (lower.contains("tài khoản") || lower.contains("mật khẩu") || lower.contains("email")) return "tai-khoan";
        if (lower.contains("dịch vụ") || lower.contains("đoàn") || lower.contains("hội")) return "dich-vu";
        return "hoc-vu";
    }
}

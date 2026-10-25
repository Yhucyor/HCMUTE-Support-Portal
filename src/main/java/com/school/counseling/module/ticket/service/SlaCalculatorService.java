package com.school.counseling.module.ticket.service;

import com.school.counseling.module.ticket.entity.TicketCategoryEnum;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Động cơ tính toán cam kết thời gian xử lý (SLA Engine)
 * Tự động suy diễn Mức độ ưu tiên và Hạn chót xử lý dựa trên Loại thủ tục học vụ.
 */
@Service
public class SlaCalculatorService {

    /**
     * Tự động suy diễn mức độ ưu tiên từ Loại thủ tục học vụ (Category)
     */
    public String resolvePriority(String categoryCode) {
        TicketCategoryEnum category = TicketCategoryEnum.fromCode(categoryCode);
        return category.getPriority();
    }

    /**
     * Tính toán hạn chót xử lý (due_date) dựa theo Loại thủ tục học vụ (Category)
     */
    public LocalDateTime calculateDueDateByCategory(String categoryCode) {
        TicketCategoryEnum category = TicketCategoryEnum.fromCode(categoryCode);
        return LocalDateTime.now().plusHours(category.getSlaHours());
    }

    /**
     * Tính toán hạn chót xử lý (due_date) dựa theo mức độ ưu tiên
     * @param priority Mức độ ưu tiên: URGENT, HIGH, MEDIUM, LOW
     * @return LocalDateTime hạn chót
     */
    public LocalDateTime calculateDueDate(String priority) {
        LocalDateTime now = LocalDateTime.now();
        if (priority == null) {
            return now.plusHours(72);
        }

        return switch (priority.toUpperCase()) {
            case "URGENT" -> now.plusHours(24);   // Khẩn cấp: 24 giờ
            case "HIGH" -> now.plusHours(48);     // Cao: 48 giờ
            case "MEDIUM" -> now.plusHours(72);   // Trung bình: 72 giờ (3 ngày)
            case "LOW" -> now.plusHours(120);     // Thấp: 120 giờ (5 ngày)
            default -> now.plusHours(72);
        };
    }

    public boolean isOverdue(LocalDateTime dueDate, String status) {
        if (dueDate == null || "RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            return false;
        }
        return LocalDateTime.now().isAfter(dueDate);
    }

    public boolean isDueSoon(LocalDateTime dueDate, String status) {
        if (dueDate == null || "RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(dueDate) && dueDate.isBefore(now.plusHours(12));
    }
}


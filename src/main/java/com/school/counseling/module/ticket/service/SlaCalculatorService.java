package com.school.counseling.module.ticket.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Động cơ tính toán cam kết thời gian xử lý (SLA Engine)
 */
@Service
public class SlaCalculatorService {

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
            case "LOW" -> now.plusDays(7);        // Thấp: 7 ngày
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
        return now.isBefore(dueDate) && dueDate.isBefore(now.plusHours(24));
    }
}

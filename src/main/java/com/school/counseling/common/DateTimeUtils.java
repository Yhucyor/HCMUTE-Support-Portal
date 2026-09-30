package com.school.counseling.common;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích xử lý định dạng ngày giờ và tính toán thời gian tương đối ("Time Ago", hạn SLA) cho QAUTE Portal.
 */
public final class DateTimeUtils {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeUtils() {
        // Utility class
    }

    /**
     * Định dạng LocalDateTime thành chuỗi "dd/MM/yyyy HH:mm".
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * Định dạng LocalDate thành chuỗi "dd/MM/yyyy".
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }

    /**
     * Hiển thị khoảng thời gian tương đối kiểu mạng xã hội ("Vừa xong", "15 phút trước", "2 giờ trước",...).
     *
     * @param target Thời điểm trong quá khứ
     * @return Chuỗi mô tả thời gian tương đối tiếng Việt
     */
    public static String timeAgo(LocalDateTime target) {
        if (target == null) return "";

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(target, now);

        long seconds = duration.getSeconds();
        if (seconds < 0) {
            return formatDateTime(target);
        }

        if (seconds < 60) {
            return "Vừa xong";
        }

        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " phút trước";
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " giờ trước";
        }

        long days = duration.toDays();
        if (days == 1) {
            return "Hôm qua lúc " + target.format(TIME_FORMATTER);
        }
        if (days < 7) {
            return days + " ngày trước";
        }

        return target.format(DATE_TIME_FORMATTER);
    }

    /**
     * Tính toán thời gian SLA còn lại hoặc đã quá hạn cho Ticket.
     *
     * @param deadline Thời hạn chót xử lý
     * @return Chuỗi thông báo SLA (VD: "Còn 14 giờ", "Đã quá hạn 2 giờ")
     */
    public static String calculateSlaRemaining(LocalDateTime deadline) {
        if (deadline == null) return "Không áp dụng SLA";

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, deadline);

        if (duration.isNegative()) {
            Duration overdue = duration.abs();
            long hours = overdue.toHours();
            long minutes = overdue.toMinutesPart();
            return String.format("Đã quá hạn %d giờ %d phút", hours, minutes);
        } else {
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            if (hours > 24) {
                long days = duration.toDays();
                return String.format("Còn %d ngày %d giờ", days, hours % 24);
            }
            return String.format("Còn lại %d giờ %d phút", hours, minutes);
        }
    }
}

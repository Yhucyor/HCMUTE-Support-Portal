package com.school.counseling.module.ticket;

import com.school.counseling.module.ticket.service.SlaCalculatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UNIT TEST - MODULE TICKET: Tính toán hạn chót SLA và cảnh báo quá hạn")
class SlaCalculatorServiceTest {

    private final SlaCalculatorService slaCalculatorService = new SlaCalculatorService();

    @Test
    @DisplayName("Tự động suy diễn mức độ ưu tiên từ loại thủ tục học vụ")
    void testResolvePriority() {
        assertEquals("URGENT", slaCalculatorService.resolvePriority("DEFER_EXAM"));
        assertEquals("URGENT", slaCalculatorService.resolvePriority("ACADEMIC_SUSPENSION"));
        assertEquals("HIGH", slaCalculatorService.resolvePriority("LMS_PORTAL_LOCK"));
        // Null hoặc mã không xác định rơi về mặc định LOW (GENERAL_INQUIRY)
        assertEquals("LOW", slaCalculatorService.resolvePriority(null));
        assertEquals("LOW", slaCalculatorService.resolvePriority("UNKNOWN_PROCEDURE"));
    }

    @Test
    @DisplayName("Tính toán hạn chót theo Category: URGENT đúng 24h")
    void testCalculateDueDateByCategory() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime dueDate = slaCalculatorService.calculateDueDateByCategory("DEFER_EXAM");
        LocalDateTime after = LocalDateTime.now();

        assertTrue(dueDate.isAfter(before.plusHours(23)));
        assertTrue(dueDate.isBefore(after.plusHours(25)));
    }

    @Test
    @DisplayName("Kiểm tra trạng thái quá hạn (isOverdue): Quá hạn khi now > dueDate và chưa đóng/giải quyết")
    void testIsOverdue() {
        LocalDateTime pastDate = LocalDateTime.now().minusHours(2);
        LocalDateTime futureDate = LocalDateTime.now().plusHours(5);

        // Đang xử lý và quá hạn
        assertTrue(slaCalculatorService.isOverdue(pastDate, "IN_PROGRESS"));
        assertTrue(slaCalculatorService.isOverdue(pastDate, "OPEN"));

        // Chưa quá hạn
        assertFalse(slaCalculatorService.isOverdue(futureDate, "IN_PROGRESS"));

        // Đã giải quyết (RESOLVED) hoặc đã đóng (CLOSED) thì không tính quá hạn
        assertFalse(slaCalculatorService.isOverdue(pastDate, "RESOLVED"));
        assertFalse(slaCalculatorService.isOverdue(pastDate, "CLOSED"));
    }

    @Test
    @DisplayName("Kiểm tra sắp đến hạn trong vòng 12h (isDueSoon)")
    void testIsDueSoon() {
        LocalDateTime dueIn6Hours = LocalDateTime.now().plusHours(6);
        LocalDateTime dueIn24Hours = LocalDateTime.now().plusHours(24);
        LocalDateTime alreadyPast = LocalDateTime.now().minusMinutes(5);

        assertTrue(slaCalculatorService.isDueSoon(dueIn6Hours, "IN_PROGRESS"));
        assertFalse(slaCalculatorService.isDueSoon(dueIn24Hours, "IN_PROGRESS"));
        assertFalse(slaCalculatorService.isDueSoon(alreadyPast, "IN_PROGRESS"));
        assertFalse(slaCalculatorService.isDueSoon(dueIn6Hours, "CLOSED"));
    }
}

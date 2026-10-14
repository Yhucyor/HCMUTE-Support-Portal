package com.school.counseling.module.ticket.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.ticket.dto.TicketResponseDto;
import com.school.counseling.module.ticket.dto.TicketSummaryDto;
import com.school.counseling.module.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST CONTROLLER DÀNH CHO CÁN BỘ & QUẢN TRỊ VIÊN (STAFF TICKET MANAGEMENT)
 * 
 * Expose các API cho bàn làm việc Cán bộ:
 * - GET /api/v1/staff/tickets: Lấy danh sách Ticket theo Khoa/Phòng
 * - POST /api/v1/staff/tickets/{ticketId}/claim: Tiếp nhận Ticket bằng cơ chế Atomic Update (chống Race Condition)
 * - POST /api/v1/staff/tickets/{ticketId}/resolve: Đánh dấu giải quyết hoàn tất Ticket
 * - GET /api/v1/staff/tickets/unread-summary: Đếm số lượng Ticket có tin nhắn phản hồi mới từ sinh viên
 */
@RestController
@RequestMapping("/api/v1/staff/tickets")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@RequiredArgsConstructor
public class StaffTicketController {

    private final TicketService ticketService;

    /**
     * Lấy danh sách Ticket thuộc Khoa/Phòng của Cán bộ (hoặc tất cả nếu là Admin)
     */
    @GetMapping
    @PreAuthorize("#departmentId == null or @deptSecurity.canAccessDepartment(#departmentId)")
    public ResponseEntity<ApiResponse<List<TicketSummaryDto>>> getStaffTickets(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status) {

        Long currentDeptId = departmentId;
        if (currentDeptId == null) {
            currentDeptId = SecurityUtils.getCurrentDepartmentId()
                    .orElseThrow(() -> new IllegalArgumentException("Cán bộ chưa được gán Đơn vị/Khoa quản lý"));
        }

        List<TicketSummaryDto> tickets = ticketService.getTicketSummaryByDepartment(currentDeptId, status);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    /**
     * Cán bộ tiếp nhận Ticket (Sử dụng câu lệnh Atomic Claim chống tranh chấp xử lý đồng thời)
     */
    @PostMapping("/{ticketId}/claim")
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public ResponseEntity<ApiResponse<TicketResponseDto>> claimTicket(@PathVariable Long ticketId) {
        Long staffId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("Cán bộ chưa đăng nhập"));
        TicketResponseDto response = ticketService.claimTicket(ticketId, staffId);
        return ResponseEntity.ok(ApiResponse.success(response, "Tiếp nhận Ticket thành công"));
    }

    /**
     * Cán bộ giải quyết hoàn tất Ticket
     */
    @PostMapping("/{ticketId}/resolve")
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public ResponseEntity<ApiResponse<TicketResponseDto>> resolveTicket(
            @PathVariable Long ticketId,
            @RequestParam(value = "note", required = false) String note) {

        Long staffId = SecurityUtils.getCurrentUserId().orElse(null);
        TicketResponseDto response = ticketService.resolveTicket(ticketId, staffId, note);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã hoàn tất giải quyết Ticket"));
    }

    /**
     * Đếm số lượng Ticket có phản hồi mới từ sinh viên đang chờ Cán bộ xem
     */
    @GetMapping("/unread-summary")
    public ResponseEntity<ApiResponse<Long>> getUnreadStudentRepliesCount() {
        boolean isAdmin = SecurityUtils.getCurrentUserPrincipal()
                .map(u -> "ROLE_ADMIN".equalsIgnoreCase(u.getRole()))
                .orElse(false);
        Long currentDeptId = isAdmin ? null : SecurityUtils.getCurrentDepartmentId().orElse(null);
        long count = ticketService.countTicketsWithStudentReply(currentDeptId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}

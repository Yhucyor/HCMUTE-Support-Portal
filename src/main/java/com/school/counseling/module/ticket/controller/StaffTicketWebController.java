package com.school.counseling.module.ticket.controller;

import com.school.counseling.common.exception.TicketAlreadyClaimedException;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.admin.dto.DepartmentAdminDto;
import com.school.counseling.module.admin.service.AdminDepartmentService;
import com.school.counseling.module.ticket.dto.StaffDashboardDataDto;
import com.school.counseling.module.ticket.dto.TicketResponseDto;
import com.school.counseling.module.ticket.dto.TicketSummaryDto;
import com.school.counseling.module.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * WEB CONTROLLER ĐIỀU HƯỚNG BÀN LÀM VIỆC CỦA CÁN BỘ & ADMIN (STAFF DASHBOARD)
 * 
 * Các tuyến đường giao diện:
 * - GET /staff/tickets: Dashboard phân chia số lượng theo trạng thái (OPEN, IN_PROGRESS, OVERDUE, RESOLVED, NEW_REPLY)
 * - POST /staff/tickets/{ticketId}/claim: Thao tác nhận vé trực tiếp từ giao diện Web
 * - POST /staff/tickets/{ticketId}/resolve: Thao tác giải quyết hoàn tất vé từ giao diện Web
 */
@Controller
@RequestMapping("/staff/tickets")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@RequiredArgsConstructor
public class StaffTicketWebController {

    private final TicketService ticketService;
    private final AdminDepartmentService adminDepartmentService;

    @GetMapping
    @PreAuthorize("#departmentId == null or @deptSecurity.canAccessDepartment(#departmentId)")
    public String staffDashboard(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            Model model) {

        boolean isAdmin = SecurityUtils.getCurrentUserPrincipal()
                .map(u -> "ROLE_ADMIN".equalsIgnoreCase(u.getRole()))
                .orElse(false);

        Long currentDeptId = departmentId;
        if (currentDeptId == null && !isAdmin) {
            currentDeptId = SecurityUtils.getCurrentDepartmentId().orElse(null);
        }

        if (currentDeptId == null && !isAdmin) {
            model.addAttribute("errorMessage", "Tài khoản cán bộ chưa được gán Đơn vị/Khoa quản lý.");
            return "error/403";
        }

        List<DepartmentAdminDto> allDepartments = adminDepartmentService.getAllDepartments();
        model.addAttribute("allDepartments", allDepartments);
        model.addAttribute("isAdmin", isAdmin);

        DepartmentAdminDto dept = null;
        if (currentDeptId != null) {
            dept = adminDepartmentService.findDepartmentDtoById(currentDeptId).orElse(null);
        }

        StaffDashboardDataDto dashboardData = ticketService.getStaffDashboardData(currentDeptId, status, page, size);

        model.addAttribute("currentDepartment", dept);
        model.addAttribute("tickets", dashboardData.getTickets());
        model.addAttribute("ticketPage", dashboardData.getTicketPage());
        model.addAttribute("currentPage", dashboardData.getCurrentPage());
        model.addAttribute("totalPages", dashboardData.getTotalPages());
        model.addAttribute("totalElements", dashboardData.getTotalElements());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("countOpen", dashboardData.getCountOpen());
        model.addAttribute("countInProgress", dashboardData.getCountInProgress());
        model.addAttribute("countOverdue", dashboardData.getCountOverdue());
        model.addAttribute("countResolved", dashboardData.getCountResolved());
        model.addAttribute("countNewReplies", dashboardData.getCountNewReplies());

        return "ticket/staff-dashboard";
    }

    @PostMapping("/{ticketId}/claim")
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public String handleClaimTicket(@PathVariable Long ticketId, RedirectAttributes redirectAttributes) {
        Long staffId = SecurityUtils.getCurrentUserId().orElse(null);
        try {
            TicketResponseDto ticket = ticketService.claimTicket(ticketId, staffId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Bạn đã tiếp nhận xử lý yêu cầu #" + ticket.getTicketCode());
            return "redirect:/tickets/detail/" + ticketId;

        } catch (TicketAlreadyClaimedException e) {
            // Race Condition Guard: Cán bộ khác đã nhận vé trước trong cùng 1 tích tắc
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/staff/tickets";
        }
    }

    @PostMapping("/{ticketId}/resolve")
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public String handleResolveTicket(
            @PathVariable Long ticketId,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "closeTicket", required = false, defaultValue = "false") boolean closeTicket,
            RedirectAttributes redirectAttributes) {

        Long staffId = SecurityUtils.getCurrentUserId().orElse(null);
        TicketResponseDto ticket = ticketService.resolveTicket(ticketId, staffId, note);

        if (closeTicket) {
            ticketService.closeTicket(ticketId, staffId, null, "Cán bộ đã giải quyết và đóng phiên tư vấn.");
            redirectAttributes.addFlashAttribute("successMessage", "Đã giải quyết và đóng yêu cầu #" + ticket.getTicketCode() + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Đã đánh dấu hoàn tất giải quyết yêu cầu #" + ticket.getTicketCode());
        }
        return "redirect:/tickets/detail/" + ticketId;
    }

    /**
     * Quản trị viên (Admin) can thiệp khẩn cấp tiếp quản vé quá hạn SLA từ các đơn vị.
     */
    @PostMapping("/{ticketId}/takeover")
    @PreAuthorize("hasRole('ADMIN')")
    public String handleAdminTakeover(@PathVariable Long ticketId, RedirectAttributes redirectAttributes) {
        Long adminId = SecurityUtils.getCurrentUserId().orElse(null);
        try {
            ticketService.adminTakeoverTicket(ticketId, adminId);
            redirectAttributes.addFlashAttribute("successMessage", "Quản trị viên đã tiếp quản xử lý Ticket quá hạn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tiếp quản vé: " + e.getMessage());
        }
        return "redirect:/staff/tickets?status=IN_PROGRESS";
    }

    /**
     * Chuyển giao Ticket sang Khoa/Phòng ban khác từ Web UI
     */
    @PostMapping("/{ticketId}/transfer")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'HEAD', 'DEPARTMENT_HEAD', 'DEPT_HEAD')")
    public String handleTransferTicket(
            @PathVariable Long ticketId,
            @RequestParam("toDepartmentId") Long toDepartmentId,
            @RequestParam(value = "reason", required = false) String reason,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.school.counseling.module.auth.dto.UserPrincipal currentUser,
            RedirectAttributes redirectAttributes) {

        try {
            com.school.counseling.module.ticket.dto.TicketTransferRequest request = com.school.counseling.module.ticket.dto.TicketTransferRequest.builder()
                    .toDepartmentId(toDepartmentId)
                    .reason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Chuyển giao chuyên môn liên phòng ban")
                    .build();

            TicketResponseDto ticket = ticketService.transferTicket(ticketId, request, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chuyển giao thành công yêu cầu #" + ticket.getTicketCode() + " sang đơn vị mới!");
            return "redirect:/staff/tickets";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể chuyển giao vé: " + e.getMessage());
            return "redirect:/tickets/detail/" + ticketId;
        }
    }
}

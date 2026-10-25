package com.school.counseling.module.ticket.controller;

import com.school.counseling.common.exception.TicketAlreadyClaimedException;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.DepartmentRepository;
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
    private final DepartmentRepository departmentRepository;

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

        List<Department> allDepartments = departmentRepository.findAll();
        model.addAttribute("allDepartments", allDepartments);
        model.addAttribute("isAdmin", isAdmin);

        Department dept = null;
        List<TicketSummaryDto> allTickets;
        List<TicketSummaryDto> filteredTickets;

        if (currentDeptId != null) {
            dept = departmentRepository.findById(currentDeptId).orElse(null);
            allTickets = ticketService.getTicketSummaryByDepartment(currentDeptId, "ALL");
        } else {
            // Admin xem toàn bộ vé toàn trường
            allTickets = ticketService.getAllTicketSummaries("ALL");
        }

        long countOpen       = allTickets.stream().filter(t -> "OPEN".equalsIgnoreCase(t.getStatus())).count();
        long countInProgress = allTickets.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long countOverdue    = allTickets.stream().filter(TicketSummaryDto::isOverdue).count();
        long countResolved   = allTickets.stream().filter(t -> "RESOLVED".equalsIgnoreCase(t.getStatus()) || "CLOSED".equalsIgnoreCase(t.getStatus())).count();
        long countNewReplies = allTickets.stream().filter(TicketSummaryDto::getHasStudentReply).count();

        if ("ALL".equalsIgnoreCase(status)) {
            filteredTickets = allTickets;
        } else if ("NEW_REPLY".equalsIgnoreCase(status)) {
            filteredTickets = allTickets.stream().filter(TicketSummaryDto::getHasStudentReply).toList();
        } else if ("OVERDUE".equalsIgnoreCase(status)) {
            filteredTickets = allTickets.stream().filter(TicketSummaryDto::isOverdue).toList();
        } else {
            filteredTickets = allTickets.stream().filter(t -> status.equalsIgnoreCase(t.getStatus())).toList();
        }

        // ─── Phân trang danh sách vé hiển thị ───
        int totalElements = filteredTickets.size();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);
        List<TicketSummaryDto> pagedContent = filteredTickets.subList(fromIndex, toIndex);

        org.springframework.data.domain.Page<TicketSummaryDto> ticketPage =
                new org.springframework.data.domain.PageImpl<>(pagedContent, org.springframework.data.domain.PageRequest.of(safePage, safeSize), totalElements);

        model.addAttribute("currentDepartment", dept);
        model.addAttribute("tickets", ticketPage.getContent());
        model.addAttribute("ticketPage", ticketPage);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", ticketPage.getTotalPages());
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("countOpen", countOpen);
        model.addAttribute("countInProgress", countInProgress);
        model.addAttribute("countOverdue", countOverdue);
        model.addAttribute("countResolved", countResolved);
        model.addAttribute("countNewReplies", countNewReplies);

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
}

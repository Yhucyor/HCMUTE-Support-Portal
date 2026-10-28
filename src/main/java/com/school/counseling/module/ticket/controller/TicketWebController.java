package com.school.counseling.module.ticket.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.ticket.dto.CreateTicketRequest;
import com.school.counseling.module.ticket.dto.TicketReplyRequest;
import com.school.counseling.module.ticket.dto.TicketResponseDto;
import com.school.counseling.module.ticket.dto.TicketSummaryDto;
import com.school.counseling.module.ticket.entity.TicketCategoryEnum;
import com.school.counseling.module.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * WEB CONTROLLER ĐIỀU HƯỚNG GIAO DIỆN THYMELEAF CHO TICKET
 * 
 * Các tuyến đường giao diện (Views Routing):
 * - GET /tickets/create: Trang form gửi yêu cầu hỗ trợ học vụ
 * - POST /tickets/create: Xử lý submit form tạo vé
 * - GET /tickets/my-tickets: Trang danh sách vé của sinh viên (Sử dụng TicketSummaryDto nhẹ 1 query)
 * - GET /tickets/detail/{ticketId}: Chi tiết vé trao đổi 2 chiều
 * - GET /tickets/guest-track: Tra cứu vé vãng lai không cần đăng nhập
 */
@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketWebController {

    private final TicketService ticketService;

    @GetMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public String createTicketPage(@RequestParam(value = "title", required = false, defaultValue = "") String title, Model model) {
        model.addAttribute("departments", ticketService.getActiveDepartments());
        model.addAttribute("initialTitle", title);
        model.addAttribute("categories", TicketCategoryEnum.values());
        return "ticket/create";
    }

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public String handleCreateTicket(
            @Valid @ModelAttribute CreateTicketRequest request,
            BindingResult bindingResult,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", ticketService.getActiveDepartments());
            model.addAttribute("categories", TicketCategoryEnum.values());
            model.addAttribute("initialTitle", request.getTitle());
            return "ticket/create";
        }

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        TicketResponseDto created = ticketService.createTicket(request, currentUserId, files);

        if (created.getGuestToken() != null) {
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đã được gửi thành công! Mã tra cứu: " + created.getTicketCode());
            return "redirect:/tickets/guest-track?token=" + created.getGuestToken();
        }

        redirectAttributes.addFlashAttribute("successMessage", "Tạo yêu cầu tư vấn thành công! Mã: " + created.getTicketCode());
        return "redirect:/tickets/my-tickets";
    }

    @GetMapping({"", "/", "/my-tickets", "/list"})
    @PreAuthorize("isAuthenticated()")
    public String myTicketsPage(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "keyword", required = false) String keyword,
            Model model) {

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        Page<TicketResponseDto> tickets = ticketService.getMyTicketsPaged(currentUserId, status, departmentId, keyword, pageable);
        Map<String, Long> statusCounts = ticketService.getMyTicketStatusCounts(currentUserId);

        model.addAttribute("tickets", tickets);
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("totalCount", statusCounts.getOrDefault("total", 0L));
        model.addAttribute("pendingCount", statusCounts.getOrDefault("pending", 0L));
        model.addAttribute("processingCount", statusCounts.getOrDefault("processing", 0L));
        model.addAttribute("waitingCount", statusCounts.getOrDefault("waiting", 0L));
        model.addAttribute("resolvedCount", statusCounts.getOrDefault("resolved", 0L));
        model.addAttribute("closedCount", statusCounts.getOrDefault("closed", 0L));

        model.addAttribute("currentStatus", status != null ? status : "ALL");
        model.addAttribute("selectedDept", departmentId);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("departments", ticketService.getActiveDepartments());
        SecurityUtils.getCurrentUser().ifPresent(u -> model.addAttribute("currentUser", u));

        return "ticket/my-tickets";
    }

    @GetMapping("/guest-track")
    public String guestTrackPage(@RequestParam("token") String token, Model model) {
        TicketResponseDto ticket = ticketService.getTicketByGuestToken(token);
        model.addAttribute("ticket", ticket);
        return "ticket/guest-track";
    }

    @PostMapping("/guest-track/rate")
    public String handleGuestRateTicket(
            @RequestParam("token") String token,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "feedback", required = false) String feedback,
            RedirectAttributes redirectAttributes) {

        ticketService.rateTicketByGuest(token, rating, feedback);
        redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã gửi đánh giá hài lòng cho buổi tư vấn!");
        return "redirect:/tickets/guest-track?token=" + token;
    }

    @ResponseBody
    @PostMapping("/guest-track/api/rate")
    public ResponseEntity<ApiResponse<TicketResponseDto>> handleGuestRateTicketAjax(
            @RequestParam("token") String token,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "feedback", required = false) String feedback) {

        TicketResponseDto ticket = ticketService.rateTicketByGuest(token, rating, feedback);
        return ResponseEntity.ok(ApiResponse.success(ticket, "Cảm ơn bạn đã gửi đánh giá hài lòng!"));
    }

    @GetMapping("/detail/{ticketId}")
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public String ticketDetailPage(@PathVariable Long ticketId, Model model) {
        TicketResponseDto ticket = ticketService.getTicketById(ticketId);
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        boolean isStaffOrAdmin = SecurityUtils.hasAnyRole("ROLE_STAFF", "ROLE_ADMIN");
        if (isStaffOrAdmin) {
            ticketService.markStaffSeen(ticketId, currentUserId);
        }
        model.addAttribute("ticket", ticket);
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("isStaffOrAdmin", isStaffOrAdmin);
        model.addAttribute("departments", ticketService.getActiveDepartments());
        return "ticket/detail";
    }

    @PostMapping("/{ticketId}/reply")
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public String handleReplyTicket(
            @PathVariable Long ticketId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        String safeContent = content != null ? content.trim() : "";
        boolean hasFiles = files != null && files.stream().anyMatch(f -> f != null && !f.isEmpty());

        if (!safeContent.isEmpty() || hasFiles) {
            Long responderId = SecurityUtils.getCurrentUserId().orElse(null);
            TicketReplyRequest request = TicketReplyRequest.builder().content(safeContent).build();
            ticketService.replyTicket(ticketId, request, responderId, files);
        }

        return "redirect:/tickets/detail/" + ticketId;
    }

    @PostMapping("/{ticketId}/close")
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public String handleCloseTicket(
            @PathVariable Long ticketId,
            @RequestParam(value = "rating", required = false, defaultValue = "5") Integer rating,
            @RequestParam(value = "feedback", required = false) String feedback,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ticketService.closeTicket(ticketId, currentUserId, rating, feedback);

        redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã gửi đánh giá hài lòng CSAT và xác nhận đóng yêu cầu!");
        return "redirect:/tickets/detail/" + ticketId;
    }

    @PostMapping("/{ticketId}/rate")
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public String handleRateTicket(
            @PathVariable Long ticketId,
            @RequestParam(value = "rating") Integer rating,
            @RequestParam(value = "feedback", required = false) String feedback,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        ticketService.rateTicket(ticketId, currentUserId, rating, feedback);

        redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận đánh giá hài lòng CSAT (" + rating + " sao). Cảm ơn ý kiến đóng góp của bạn!");
        return "redirect:/tickets/detail/" + ticketId;
    }
}

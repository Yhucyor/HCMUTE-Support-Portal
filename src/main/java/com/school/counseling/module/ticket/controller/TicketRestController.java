package com.school.counseling.module.ticket.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.ticket.dto.CreateTicketRequest;
import com.school.counseling.module.ticket.dto.TicketReplyRequest;
import com.school.counseling.module.ticket.dto.TicketResponseDto;
import com.school.counseling.module.ticket.dto.TicketSummaryDto;
import com.school.counseling.module.ticket.dto.TicketTriageResponseDto;
import com.school.counseling.module.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST CONTROLLER XỬ LÝ PHIẾU HỖ TRỢ (TICKETS)
 * 
 * Expose các API RESTful JSON chuẩn hóa ApiResponse<T>:
 * - /api/v1/tickets/ai-triage: Tự động phân loại đơn vị & mức ưu tiên
 * - /api/v1/tickets: Tạo vé mới (hỗ trợ multipart upload tệp đính kèm)
 * - /api/v1/tickets/my-tickets: Lấy danh sách tóm tắt vé của sinh viên đang đăng nhập
 * - /api/v1/tickets/{ticketId}: Xem chi tiết vé (được bảo vệ SpEL chống IDOR)
 * - /api/v1/tickets/{ticketId}/reply: Phản hồi vé kèm file đính kèm
 * - /api/v1/tickets/{ticketId}/close & rate: Đóng vé & ghi nhận đánh giá CSAT
 * - /api/v1/tickets/guest-track: Tra cứu vé vãng lai qua guest token bí mật
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketRestController {

    private final TicketService ticketService;

    public record TriageRequest(String title, String description) {}

    /**
     * Endpoint AI Triage: Phân tích tiêu đề và nội dung để gợi ý Đơn vị và Mức độ ưu tiên.
     */
    @PostMapping("/ai-triage")
    public ResponseEntity<ApiResponse<TicketTriageResponseDto>> triageTicket(
            @RequestBody(required = false) TriageRequest body,
            @RequestParam(value = "title", required = false) String titleParam,
            @RequestParam(value = "description", required = false) String descParam) {

        String title = (body != null && body.title() != null) ? body.title() : (titleParam != null ? titleParam : "");
        String description = (body != null && body.description() != null) ? body.description() : (descParam != null ? descParam : "");

        TicketTriageResponseDto triage = ticketService.suggestTicketTriage(title, description);
        return ResponseEntity.ok(ApiResponse.success(triage, "Phân loại và định tuyến Ticket thành công"));
    }

    /**
     * Tạo mới Ticket (hỗ trợ cả Sinh viên có tài khoản và Khách vãng lai Guest)
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<TicketResponseDto>> createTicket(
            @Valid @ModelAttribute CreateTicketRequest request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        TicketResponseDto response = ticketService.createTicket(request, currentUserId, files);
        return ResponseEntity.ok(ApiResponse.success(response, "Gửi yêu cầu tư vấn thành công"));
    }

    /**
     * Lấy danh sách tóm tắt Ticket của sinh viên đang đăng nhập
     */
    @GetMapping("/my-tickets")
    public ResponseEntity<ApiResponse<List<TicketSummaryDto>>> getMyTickets() {
        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalArgumentException("Người dùng chưa đăng nhập"));
        List<TicketSummaryDto> tickets = ticketService.getMyTicketSummaries(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    /**
     * Xem thông tin chi tiết của một Ticket (được bảo vệ SpEL chống IDOR qua @deptSecurity)
     */
    @GetMapping("/{ticketId}")
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public ResponseEntity<ApiResponse<TicketResponseDto>> getTicketDetail(@PathVariable Long ticketId) {
        TicketResponseDto ticket = ticketService.getTicketById(ticketId);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    /**
     * Gửi phản hồi / trao đổi trong Ticket (hỗ trợ đính kèm nhiều file)
     */
    @PostMapping(value = "/{ticketId}/reply", consumes = {"multipart/form-data"})
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public ResponseEntity<ApiResponse<TicketResponseDto>> replyTicket(
            @PathVariable Long ticketId,
            @Valid @ModelAttribute TicketReplyRequest request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        boolean hasContent = request != null && request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasFiles = files != null && !files.isEmpty() && files.stream().anyMatch(f -> f != null && !f.isEmpty());

        if (!hasContent && !hasFiles) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng nhập nội dung hoặc đính kèm tệp tin/hình ảnh"));
        }

        Long responderId = SecurityUtils.getCurrentUserId().orElse(null);
        TicketResponseDto response = ticketService.replyTicket(ticketId, request, responderId, files);
        return ResponseEntity.ok(ApiResponse.success(response, "Gửi phản hồi thành công"));
    }

    /**
     * Đóng Ticket và gửi kèm đánh giá chất lượng phục vụ (CSAT)
     */
    @PostMapping("/{ticketId}/close")
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public ResponseEntity<ApiResponse<TicketResponseDto>> closeTicket(
            @PathVariable Long ticketId,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "feedback", required = false) String feedback) {

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        TicketResponseDto response = ticketService.closeTicket(ticketId, currentUserId, rating, feedback);
        return ResponseEntity.ok(ApiResponse.success(response, "Đóng yêu cầu tư vấn thành công"));
    }

    /**
     * Ghi nhận đánh giá CSAT (1 - 5 sao) cho Ticket đã được giải quyết
     */
    @PostMapping("/{ticketId}/rate")
    @PreAuthorize("@deptSecurity.canAccessTicket(#ticketId)")
    public ResponseEntity<ApiResponse<TicketResponseDto>> rateTicket(
            @PathVariable Long ticketId,
            @RequestParam(value = "rating") Integer rating,
            @RequestParam(value = "feedback", required = false) String feedback) {

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        TicketResponseDto response = ticketService.rateTicket(ticketId, currentUserId, rating, feedback);
        return ResponseEntity.ok(ApiResponse.success(response, "Ghi nhận đánh giá CSAT thành công"));
    }

    /**
     * Tra cứu tiến độ xử lý yêu cầu dành cho Khách vãng lai qua Access Token bí mật
     */
    @GetMapping("/guest-track")
    public ResponseEntity<ApiResponse<TicketResponseDto>> trackGuestTicket(@RequestParam("token") String token) {
        TicketResponseDto ticket = ticketService.getTicketByGuestToken(token);
        return ResponseEntity.ok(ApiResponse.success(ticket, "Tra cứu tiến độ thành công"));
    }
}

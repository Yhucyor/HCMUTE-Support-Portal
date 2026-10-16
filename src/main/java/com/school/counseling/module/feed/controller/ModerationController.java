package com.school.counseling.module.feed.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.feed.dto.PostReportResponseDto;
import com.school.counseling.module.feed.dto.PostResponseDto;
import com.school.counseling.module.feed.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/moderation")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@RequiredArgsConstructor
public class ModerationController {

    private final PostService postService;
    private final UserRepository userRepository;

    /**
     * Hàng đợi duyệt bài thảo luận của Sinh viên
     */
    @GetMapping("/posts")
    public String viewPendingPosts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(Math.max(0, page), 10);
        Page<PostResponseDto> pendingPosts = postService.getPendingPostsDto(pageable);

        model.addAttribute("posts", pendingPosts);
        model.addAttribute("pendingCount", postService.countPendingPosts());
        model.addAttribute("reportCount", postService.countPendingReports());
        return "moderation/pending-posts";
    }

    /**
     * Cán bộ phê duyệt bài viết
     */
    @PostMapping("/posts/{id}/approve")
    public String approvePost(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        User staff = userRepository.findById(currentUserId).orElseThrow();

        postService.approvePost(id, staff);
        redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt bài viết #" + id + " thành công!");
        return "redirect:/moderation/posts";
    }

    /**
     * Cán bộ từ chối bài viết
     */
    @PostMapping("/posts/{id}/reject")
    public String rejectPost(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", defaultValue = "Nội dung chưa phù hợp quy chuẩn diễn đàn") String reason,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        User staff = userRepository.findById(currentUserId).orElseThrow();

        postService.rejectPost(id, reason, staff);
        redirectAttributes.addFlashAttribute("infoMessage", "Đã từ chối bài viết #" + id + " (Lý do: " + reason + ")");
        return "redirect:/moderation/posts";
    }

    /**
     * Danh sách báo cáo vi phạm bài viết
     */
    @GetMapping("/reports")
    public String viewReports(
            @RequestParam(value = "status", required = false, defaultValue = "PENDING") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(Math.max(0, page), 10);
        Page<PostReportResponseDto> reports = postService.getReportsDto(status, pageable);

        model.addAttribute("reports", reports);
        model.addAttribute("currentStatus", status);
        model.addAttribute("pendingCount", postService.countPendingPosts());
        model.addAttribute("reportCount", postService.countPendingReports());
        model.addAttribute("resolvedReportCount", postService.countResolvedReports());
        return "moderation/reports-list";
    }

    /**
     * Xử lý báo cáo vi phạm (Ẩn bài / Bỏ qua / Khóa tài khoản)
     */
    @PostMapping("/reports/{id}/resolve")
    public String resolveReport(
            @PathVariable("id") Long id,
            @RequestParam("action") String action,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        User staff = userRepository.findById(currentUserId).orElseThrow();

        postService.resolveReport(id, action, staff);

        String msg = switch (action) {
            case "HIDE_POST" -> "Đã ẩn bài viết vi phạm khỏi Diễn đàn.";
            case "LOCK_USER" -> "Đã ẩn bài viết và khóa tài khoản sinh viên vi phạm.";
            default -> "Đã bỏ qua báo cáo vi phạm.";
        };

        redirectAttributes.addFlashAttribute("successMessage", msg);
        return "redirect:/moderation/reports";
    }

    /**
     * Khôi phục / Bỏ ẩn bài viết bị báo cáo nhầm
     */
    @PostMapping("/reports/{id}/unhide")
    public String unhidePost(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        User staff = userRepository.findById(currentUserId).orElseThrow();

        postService.unhidePost(id, staff);
        redirectAttributes.addFlashAttribute("successMessage", "Đã khôi phục bài viết và cho phép hiển thị lại trên Diễn đàn!");
        return "redirect:/moderation/reports?status=RESOLVED";
    }
}

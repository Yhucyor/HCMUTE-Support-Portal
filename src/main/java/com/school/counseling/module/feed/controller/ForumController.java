package com.school.counseling.module.feed.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.auth.service.DepartmentQueryService;
import com.school.counseling.module.feed.entity.Post;
import com.school.counseling.module.feed.service.LikeService;
import com.school.counseling.module.feed.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/feed/forum")
public class ForumController {

    private final PostService postService;
    private final UserRepository userRepository;
    private final LikeService likeService;
    private final DepartmentQueryService departmentQueryService;

    public ForumController(PostService postService, UserRepository userRepository, LikeService likeService, DepartmentQueryService departmentQueryService) {
        this.postService = postService;
        this.userRepository = userRepository;
        this.likeService = likeService;
        this.departmentQueryService = departmentQueryService;
    }

    /**
     * Diễn đàn thảo luận sinh viên (Hỗ trợ B ộ lọc Khoa, Tìm kiếm và Tab "Bài viết của tôi")
     */
    @GetMapping // bat phuong thuc http get  
    // @RequestParam nhận url query  tab hiển thị  , deparmentid : loc bai viet theo khoa , q : từ khóa tìm kiếm  , page số trang hiện tại 
    // xem danh sách diễn đàn 
    public String viewStudentForum(
            @RequestParam(value = "tab", defaultValue = "all") String tab,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpServletRequest request,
            Model model) {
        request.getSession(true);
        // tạo đối tượng phân trang 10 bài viết mỗi trang 
        Pageable pageable = PageRequest.of(Math.max(0, page), 10);
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);

        // nếu chọn tab my post sẽ láy bài viết của tôi  ngược lại gọi tìm kiếm và lọc các bài viết phê duyẹt 
        Page<com.school.counseling.module.feed.dto.PostResponseDto> posts;
        if ("my_posts".equalsIgnoreCase(tab) && currentUserId != null) {
            posts = postService.getMyPostsDto(currentUserId, pageable);
        } else {
            posts = postService.searchApprovedForumPostsDto(query, departmentId, currentUserId, pageable);
            tab = "all";
        }
// ds khoa phòng đang hoạt động 
        List<Department> departments = departmentQueryService.getActiveDepartments();

    // giao dien avatar thong tin 
        if (currentUserId != null) {
            userRepository.findByIdWithRoleAndDepartment(currentUserId).ifPresent(user -> {
                model.addAttribute("currentUser", user);
                model.addAttribute("currentUserFullName", user.getFullName());
                model.addAttribute("currentUserAvatar", user.getAvatarUrl());
                String role = (user.getRole() != null) ? user.getRole().getName().replace("ROLE_", "") : "STUDENT";
                model.addAttribute("currentUserRole", role);
            });
        }

        // đẩy ds bài viết vf trạng thái bộ loc vào model 
        model.addAttribute("posts", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("currentTab", tab);
        model.addAttribute("selectedDept", departmentId);
        model.addAttribute("query", query);
        model.addAttribute("departments", departments);
        model.addAttribute("currentUserId", currentUserId);

        return "feed/forum";  // trả về render ra trình duyệt 
    }

    /**
     * Hủy / Xóa bài viết của chính tác giả hoặc Staff/Admin
     */
    @PostMapping("/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String handleDeletePost(
            @PathVariable("id") Long id,
            @RequestParam(value = "redirectTab", defaultValue = "all") String redirectTab,
            RedirectAttributes redirectAttributes) {
// kt quyèn tác giả hoặc admin ms có quyền xóa 
        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        boolean isStaffOrAdmin = SecurityUtils.hasAnyRole("STAFF", "ADMIN");

        try {
            postService.deletePost(id, currentUserId, isStaffOrAdmin);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bài viết thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa bài viết: " + e.getMessage());
        }

        return "redirect:/feed/forum" + ("my_posts".equalsIgnoreCase(redirectTab) ? "?tab=my_posts" : "");
    }

    /**
     * Giao diện trang tạo bài viết mới trên Diễn đàn
     */
    @GetMapping({"/create", "/new"})
    public String viewCreatePostPage(HttpServletRequest request, Model model) {
        request.getSession(true);
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId != null) {
            userRepository.findByIdWithRoleAndDepartment(currentUserId).ifPresent(user -> {
                model.addAttribute("currentUser", user);
                model.addAttribute("currentUserFullName", user.getFullName());
                model.addAttribute("currentUserAvatar", user.getAvatarUrl());
                String role = (user.getRole() != null) ? user.getRole().getName().replace("ROLE_", "") : "STUDENT";
                model.addAttribute("currentUserRole", role);
                model.addAttribute("currentUserDept", user.getDepartment() != null ? user.getDepartment().getName() : "Toàn trường");
            });
        }
        List<Department> departments = departmentQueryService.getActiveDepartments();
        model.addAttribute("departments", departments);
        model.addAttribute("currentUserId", currentUserId);

        return "feed/create-post";
    }

    /**
     * Sinh viên đăng bài thảo luận mới (Trạng thái PENDING_APPROVAL)
     */
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public String handleCreateForumPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "image", required = false) MultipartFile image,
            RedirectAttributes redirectAttributes) {
        // validate manual validation = tay thủ công code thuần
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tiêu đề và nội dung bài viết không được để trống.");
            return "redirect:/feed/forum/create";
        }

        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        User author = userRepository.findById(currentUserId).orElseThrow();

        // Ghép thêm tag nếu người dùng nhập vào
        StringBuilder enrichedContent = new StringBuilder(content.trim());
        if (tags != null && !tags.isBlank()) {
            enrichedContent.append("\n\n").append(tags.trim());
        }

        try {
            Post post = postService.createForumPost(title.trim(), enrichedContent.toString(), departmentId, image, author);

            if ("APPROVED".equalsIgnoreCase(post.getStatus())) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Bài viết của bạn đã được xuất bản công khai trên Diễn đàn!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Bài thảo luận đã được gửi thành công! Theo quy định, bài viết sẽ hiển thị công khai sau khi được Cán bộ phê duyệt.");
            }
            return "redirect:/feed/forum";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo bài viết: " + e.getMessage());
            return "redirect:/feed/forum/create";
        }
    }

    /**
     * Thả tim (Like) bài viết
     */
    @PostMapping("/{id}/like")
    public String handleLikePost(@PathVariable("id") Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId != null) {
            likeService.toggleLike(currentUserId, "POST", id);
        }
        return "redirect:/feed/forum#post_" + id;
    }

    /**
     * Bình luận vào bài viết
     */
    @PostMapping("/{id}/comment")
    @PreAuthorize("isAuthenticated()")
    public String handleAddComment(
            @PathVariable("id") Long id,
            @RequestParam("content") String content,
            RedirectAttributes redirectAttributes) {
// validate nội dung bình luận 
        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nội dung bình luận không được để trống.");
            return "redirect:/feed/forum#post_" + id;
        }

        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        User author = userRepository.findById(currentUserId).orElseThrow();

        postService.addComment(id, content, author);

        return "redirect:/feed/forum#post_" + id;
    }

    /**
     * Báo cáo bài viết vi phạm
     */
    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public String handleReportPost(
            @PathVariable("id") Long id,
            @RequestParam("reason") String reason,
            @RequestParam(value = "details", required = false) String details,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        User reporter = userRepository.findById(currentUserId).orElseThrow();

        postService.reportPost(id, reason, details, reporter);

        redirectAttributes.addFlashAttribute("successMessage",
                "Cảm ơn bạn đã gửi báo cáo vi phạm. Ban Quản Trị sẽ xem xét và xử lý trong thời gian sớm nhất.");
        return "redirect:/feed/forum";
    }
}

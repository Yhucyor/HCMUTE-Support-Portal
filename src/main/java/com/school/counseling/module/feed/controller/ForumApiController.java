package com.school.counseling.module.feed.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.feed.dto.CommentDto;
import com.school.counseling.module.feed.dto.CreateCommentRequest;
import com.school.counseling.module.feed.dto.LikeToggleResponseDto;
import com.school.counseling.module.feed.dto.PostResponseDto;
import com.school.counseling.module.feed.service.LikeService;
import com.school.counseling.module.feed.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST API Controller phục vụ toàn bộ tương tác AJAX Diễn Đàn Sinh Viên (Zero Page Reload)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/forum")
@RequiredArgsConstructor
public class ForumApiController {

    private final PostService postService;
    private final UserRepository userRepository;
    private final LikeService likeService;

    /**
     * Đăng bài thảo luận sinh viên mới qua Fetch API / AJAX (Không reload trang)
     */
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostResponseDto>> createPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tiêu đề và nội dung bài viết không được để trống"));
        }

        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        User author = userRepository.findByIdWithRoleAndDepartment(currentUserId).orElseThrow();

        PostResponseDto createdPost = postService.createForumPostDto(title.trim(), content.trim(), departmentId, image, author);

        String message = "APPROVED".equalsIgnoreCase(createdPost.getStatus())
                ? "Đã đăng bài thảo luận thành công!"
                : "Bài thảo luận đã gửi thành công và đang chờ Cán bộ phê duyệt theo quy định!";

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdPost, message));
    }

    /**
     * Thêm bình luận hoặc phản hồi lồng nhau (Nested Comment) qua AJAX
     */
    @PostMapping("/posts/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentDto>> addComment(
            @PathVariable("id") Long postId,
            @Valid @RequestBody CreateCommentRequest request) {

        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        User author = userRepository.findByIdWithRoleAndDepartment(currentUserId).orElseThrow();

        CommentDto commentDto = postService.addCommentDto(postId, request.getContent(), request.getParentId(), author);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(commentDto, "Đã gửi bình luận thành công!"));
    }

    /**
     * Lấy cây bình luận phân cấp (Nested Comments) của bài viết
     */
    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<ApiResponse<List<CommentDto>>> getComments(@PathVariable("id") Long postId) {
        List<CommentDto> comments = postService.getPostCommentsDto(postId);
        return ResponseEntity.ok(ApiResponse.success(comments, "Lấy danh sách bình luận thành công"));
    }

    /**
     * Xóa bài thảo luận qua AJAX (Dành cho Tác giả hoặc Staff/Admin)
     */
    @DeleteMapping("/posts/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable("id") Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        boolean isStaffOrAdmin = SecurityUtils.hasAnyRole("STAFF", "ADMIN");

        postService.deletePost(id, currentUserId, isStaffOrAdmin);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa bài viết thành công!"));
    }

    /**
     * Thả tim (Like / Unlike) bài viết qua AJAX
     */
    @PostMapping("/posts/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LikeToggleResponseDto>> likePost(@PathVariable("id") Long postId) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        LikeToggleResponseDto result = likeService.toggleLike(currentUserId, "POST", postId);
        return ResponseEntity.ok(ApiResponse.success(result, "Thao tác thả tim bài viết thành công"));
    }

    /**
     * Thả tim (Like) bình luận qua AJAX
     */
    @PostMapping("/comments/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LikeToggleResponseDto>> likeComment(@PathVariable("id") Long commentId) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        LikeToggleResponseDto result = likeService.toggleLike(currentUserId, "COMMENT", commentId);
        return ResponseEntity.ok(ApiResponse.success(result, "Thao tác thả tim bình luận thành công"));
    }
}

package com.school.counseling.module.feed.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.feed.dto.CreatePostRequest;
import com.school.counseling.module.feed.dto.PostResponseDto;
import com.school.counseling.module.feed.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class OfficialPostRestController {

    private final PostService postService;
    private final UserRepository userRepository;

    /**
     * Cán bộ đăng thông báo chính thức kèm tài liệu và cờ yêu cầu sinh video
     */
    @PostMapping("/official")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<PostResponseDto>> createOfficialPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User author = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        PostResponseDto createdPost = postService.createOfficialPost(request, author);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdPost, "Đăng thông báo chính thức thành công"));
    }

    /**
     * Lấy danh sách thông báo chính thức cho sinh viên và công chúng
     */
    @GetMapping("/official")
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> getOfficialPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostResponseDto> posts = postService.getApprovedOfficialPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success(posts, "Lấy danh sách thông báo thành công"));
    }

    /**
     * Lấy chi tiết thông báo kèm video và tài liệu đính kèm
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponseDto>> getPostDetail(@PathVariable Long id) {
        PostResponseDto post = postService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success(post, "Lấy chi tiết thông báo thành công"));
    }
}

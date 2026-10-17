package com.school.counseling.module.feed.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.feed.dto.LikeToggleResponseDto;
import com.school.counseling.module.feed.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeApiController {

    private final LikeService likeService;

    /**
     * API Thả tim / Hủy thích (Toggle Like) bằng Fetch API không tải lại trang
     */
    @PostMapping("/toggle")
    public ResponseEntity<?> toggleLike(
            @RequestParam(name = "targetType", defaultValue = "POST") String targetType,
            @RequestParam(name = "targetId") Long targetId) {

        if (!SecurityUtils.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Vui lòng đăng nhập để thực hiện thả tim."));
        }

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Không tìm thấy phiên đăng nhập."));
        }

        try {
            LikeToggleResponseDto response = likeService.toggleLike(currentUserId, targetType, targetId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi toggle like cho targetId={}: {}", targetId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
        }
    }
}

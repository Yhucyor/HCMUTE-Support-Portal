package com.school.counseling.module.feed.service;

import com.school.counseling.common.exception.ResourceNotFoundException;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.feed.dto.LikeToggleResponseDto;
import com.school.counseling.module.feed.entity.Comment;
import com.school.counseling.module.feed.entity.Like;
import com.school.counseling.module.feed.entity.Post;
import com.school.counseling.module.feed.repository.CommentRepository;
import com.school.counseling.module.feed.repository.LikeRepository;
import com.school.counseling.module.feed.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SERVICE QUẢN LÝ LƯỢT THÍCH (LIKE SERVICE)
 * 
 * Hỗ trợ thao tác Like/Unlike không reload trang cho cả Bài viết (POST) và Bình luận (COMMENT).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /**
     * Bật/Tắt trạng thái Thả tim (Like/Unlike Toggle) không tải lại trang
     */
    @Transactional
    public LikeToggleResponseDto toggleLike(Long userId, String targetType, Long targetId) {
        if (targetType == null) targetType = "POST";
        final String type = targetType.toUpperCase().trim();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng ID: " + userId));

        Optional<Like> existingLike = likeRepository.findByUserIdAndTargetTypeAndTargetId(userId, type, targetId);

        boolean liked;
        int newLikeCount = 0;

        if (existingLike.isPresent()) {
            // Đã like -> Tiến hành Unlike
            likeRepository.delete(existingLike.get());
            liked = false;

            if ("POST".equals(type)) {
                Post post = postRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết ID: " + targetId));
                int current = (post.getLikeCount() != null) ? post.getLikeCount() : 0;
                newLikeCount = Math.max(0, current - 1);
                post.setLikeCount(newLikeCount);
                postRepository.save(post);
            } else if ("COMMENT".equals(type)) {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận ID: " + targetId));
                int current = (comment.getLikeCount() != null) ? comment.getLikeCount() : 0;
                newLikeCount = Math.max(0, current - 1);
                comment.setLikeCount(newLikeCount);
                commentRepository.save(comment);
            }
        } else {
            // Chưa like -> Tạo Like mới
            Like newLike = Like.builder()
                    .user(user)
                    .targetType(type)
                    .targetId(targetId)
                    .build();
            likeRepository.save(newLike);
            liked = true;

            if ("POST".equals(type)) {
                Post post = postRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết ID: " + targetId));
                int current = (post.getLikeCount() != null) ? post.getLikeCount() : 0;
                newLikeCount = current + 1;
                post.setLikeCount(newLikeCount);
                postRepository.save(post);
            } else if ("COMMENT".equals(type)) {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận ID: " + targetId));
                int current = (comment.getLikeCount() != null) ? comment.getLikeCount() : 0;
                newLikeCount = current + 1;
                comment.setLikeCount(newLikeCount);
                commentRepository.save(comment);
            }
        }

        return LikeToggleResponseDto.builder()
                .targetType(type)
                .targetId(targetId)
                .liked(liked)
                .likeCount(newLikeCount)
                .message(liked ? "Đã thích thành công" : "Đã hủy thích")
                .build();
    }

    /**
     * Kiểm tra người dùng đã like mục này chưa
     */
    @Transactional(readOnly = true)
    public boolean hasUserLiked(Long userId, String targetType, Long targetId) {
        if (userId == null) return false;
        return likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, targetType.toUpperCase(), targetId);
    }

    /**
     * Lấy danh sách ID các bài viết/bình luận mà người dùng đã like (dùng để highlight tim đỏ)
     */
    @Transactional(readOnly = true)
    public Set<Long> getLikedTargetIds(Long userId, String targetType, Collection<Long> targetIds) {
        if (userId == null || targetIds == null || targetIds.isEmpty()) {
            return Collections.emptySet();
        }
        return likeRepository.findByUserIdAndTargetTypeAndTargetIdIn(userId, targetType.toUpperCase(), targetIds)
                .stream()
                .map(Like::getTargetId)
                .collect(Collectors.toSet());
    }
}

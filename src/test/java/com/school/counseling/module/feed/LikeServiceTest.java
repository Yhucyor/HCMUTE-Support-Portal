package com.school.counseling.module.feed;

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
import com.school.counseling.module.feed.service.LikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UNIT TEST - MODULE FEED: Kiểm thử LikeService toggle lượt thích Post và Comment")
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LikeService likeService;

    private User sampleUser;
    private Post samplePost;
    private Comment sampleComment;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).username("sv01").fullName("Sinh Vien 01").build();
        samplePost = Post.builder().id(10L).title("Bài viết thử nghiệm").likeCount(5).build();
        sampleComment = Comment.builder().id(20L).content("Bình luận hữu ích").likeCount(2).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(postRepository.findById(10L)).thenReturn(Optional.of(samplePost));
        when(commentRepository.findById(20L)).thenReturn(Optional.of(sampleComment));
    }

    @Test
    @DisplayName("Thả tim bài viết (Chưa like -> Tạo Like và tăng likeCount + 1)")
    void testToggleLike_Post_AddNewLike() {
        when(likeRepository.findByUserIdAndTargetTypeAndTargetId(1L, "POST", 10L)).thenReturn(Optional.empty());

        LikeToggleResponseDto response = likeService.toggleLike(1L, "POST", 10L);

        assertTrue(response.isLiked());
        assertEquals(6, response.getLikeCount());
        assertEquals(6, samplePost.getLikeCount());
        verify(likeRepository, times(1)).save(any(Like.class));
        verify(postRepository, times(1)).save(samplePost);
    }

    @Test
    @DisplayName("Bỏ thích bài viết (Đã like -> Xóa Like và giảm likeCount - 1)")
    void testToggleLike_Post_RemoveExistingLike() {
        Like existing = Like.builder().id(99L).user(sampleUser).targetType("POST").targetId(10L).build();
        when(likeRepository.findByUserIdAndTargetTypeAndTargetId(1L, "POST", 10L)).thenReturn(Optional.of(existing));

        LikeToggleResponseDto response = likeService.toggleLike(1L, "POST", 10L);

        assertFalse(response.isLiked());
        assertEquals(4, response.getLikeCount());
        assertEquals(4, samplePost.getLikeCount());
        verify(likeRepository, times(1)).delete(existing);
        verify(postRepository, times(1)).save(samplePost);
    }

    @Test
    @DisplayName("Bỏ thích khi likeCount = 0: Đảm bảo số like không bao giờ âm")
    void testToggleLike_Post_NeverNegative() {
        samplePost.setLikeCount(0);
        Like existing = Like.builder().id(99L).user(sampleUser).targetType("POST").targetId(10L).build();
        when(likeRepository.findByUserIdAndTargetTypeAndTargetId(1L, "POST", 10L)).thenReturn(Optional.of(existing));

        LikeToggleResponseDto response = likeService.toggleLike(1L, "POST", 10L);

        assertFalse(response.isLiked());
        assertEquals(0, response.getLikeCount());
        assertEquals(0, samplePost.getLikeCount());
    }

    @Test
    @DisplayName("Thả tim bình luận (Comment): Tăng likeCount của comment thành công")
    void testToggleLike_Comment_Success() {
        when(likeRepository.findByUserIdAndTargetTypeAndTargetId(1L, "COMMENT", 20L)).thenReturn(Optional.empty());

        LikeToggleResponseDto response = likeService.toggleLike(1L, "COMMENT", 20L);

        assertTrue(response.isLiked());
        assertEquals(3, response.getLikeCount());
        assertEquals(3, sampleComment.getLikeCount());
        verify(commentRepository, times(1)).save(sampleComment);
    }

    @Test
    @DisplayName("Thả tim thất bại: Người dùng không tồn tại")
    void testToggleLike_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                likeService.toggleLike(999L, "POST", 10L));
    }
}

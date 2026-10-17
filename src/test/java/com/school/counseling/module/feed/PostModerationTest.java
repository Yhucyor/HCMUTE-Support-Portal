package com.school.counseling.module.feed;

import com.school.counseling.common.exception.AppException;
import com.school.counseling.common.storage.IStorageService;
import com.school.counseling.module.auth.entity.Role;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.feed.entity.Post;
import com.school.counseling.module.feed.entity.PostReport;
import com.school.counseling.module.feed.repository.*;
import com.school.counseling.module.feed.service.BannedWordsFilterService;
import com.school.counseling.module.feed.service.PostService;
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

/**
 * UNIT TEST TDD: KIỂM THỬ BỘ LỌC TỪ CẤM & QUY TRÌNH KIỂM DUYỆT BÀI VIẾT (MODERATION WORKFLOW)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TDD-FEED-01: Kiểm thử Bộ lọc Từ cấm & Quy trình Duyệt bài")
class PostModerationTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostAttachmentRepository attachmentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostReportRepository postReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private IStorageService storageService;

    @Mock
    private BannedWordsFilterService bannedWordsFilterService;

    @InjectMocks
    private PostService postService;

    private User studentUser;
    private User staffUser;
    private Post pendingPost;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1L).name("ROLE_STUDENT").build();
        Role staffRole = Role.builder().id(2L).name("ROLE_STAFF").build();

        studentUser = User.builder().id(101L).username("sv01").fullName("Sinh Viên A").role(studentRole).build();
        staffUser = User.builder().id(202L).username("canbo01").fullName("Cán Bộ B").role(staffRole).build();

        pendingPost = Post.builder()
                .id(1L)
                .title("Hỏi về học phần thực tập tốt nghiệp")
                .content("Cho em hỏi điều kiện đăng ký thực tập tốt nghiệp đợt 2 là gì ạ?")
                .postType("STUDENT_FORUM")
                .status("PENDING_APPROVAL")
                .author(studentUser)
                .build();
    }

    @Test
    @DisplayName("1. Phê duyệt bài viết: Trạng thái chuyển sang APPROVED và lưu vết Cán bộ duyệt")
    void testApprovePost_Success() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(pendingPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.approvePost(1L, staffUser);

        assertEquals("APPROVED", pendingPost.getStatus());
        assertEquals(staffUser, pendingPost.getApprovedBy());
        assertNotNull(pendingPost.getApprovedAt());
        verify(postRepository, times(1)).save(pendingPost);
    }

    @Test
    @DisplayName("2. Từ chối bài viết: Trạng thái chuyển sang REJECTED và lưu lý do từ chối")
    void testRejectPost_Success() {
        String reason = "Bài viết trùng lặp nội dung với câu hỏi trước";
        when(postRepository.findById(1L)).thenReturn(Optional.of(pendingPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.rejectPost(1L, reason, staffUser);

        assertEquals("REJECTED", pendingPost.getStatus());
        assertEquals(reason, pendingPost.getRejectionReason());
        assertEquals(staffUser, pendingPost.getApprovedBy());
        assertNotNull(pendingPost.getApprovedAt());
        verify(postRepository, times(1)).save(pendingPost);
    }

    @Test
    @DisplayName("3. Xử lý báo cáo vi phạm HIDE_POST: Ẩn bài viết khỏi diễn đàn")
    void testResolveReport_HidePost() {
        PostReport report = PostReport.builder()
                .id(10L)
                .post(pendingPost)
                .reporter(studentUser)
                .reason("Nội dung không lành mạnh")
                .status("PENDING")
                .build();

        when(postReportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(postReportRepository.save(any(PostReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.resolveReport(10L, "HIDE_POST", staffUser);

        assertEquals("HIDDEN", pendingPost.getStatus());
        assertEquals("RESOLVED", report.getStatus());
        assertEquals("HIDE_POST", report.getActionTaken());
        assertEquals(staffUser, report.getResolvedBy());
        assertNotNull(report.getResolvedAt());
        verify(postRepository, times(1)).save(pendingPost);
        verify(postReportRepository, times(1)).save(report);
    }

    @Test
    @DisplayName("4. BannedWordsFilterService: Phát hiện từ cấm trực tiếp từ bộ lọc")
    void testBannedWordsFilterService_Detection() {
        BannedWordsFilterService filterService = new BannedWordsFilterService();

        // Từ khóa bình thường hợp lệ -> không ném ngoại lệ
        assertDoesNotThrow(() -> filterService.validateContent("Xin chào thầy cô và các bạn sinh viên"));

        // Từ cấm vi phạm -> ném ngoại lệ IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> filterService.validateContent("Nội dung chứa từ cấm vcl và khốn nạn"));
    }
}

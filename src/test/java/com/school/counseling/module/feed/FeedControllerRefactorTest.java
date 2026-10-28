package com.school.counseling.module.feed;

import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.auth.service.DepartmentQueryService;
import com.school.counseling.module.feed.controller.ForumController;
import com.school.counseling.module.feed.controller.OfficialFeedController;
import com.school.counseling.module.feed.entity.PostAttachment;
import com.school.counseling.module.feed.service.LikeService;
import com.school.counseling.module.feed.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedControllerRefactorTest {

    @Mock
    private PostService postService;

    @Mock
    private DepartmentQueryService departmentQueryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LikeService likeService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private OfficialFeedController officialFeedController;
    private ForumController forumController;

    @BeforeEach
    void setUp() {
        officialFeedController = new OfficialFeedController(postService, departmentQueryService, userRepository);
        forumController = new ForumController(postService, userRepository, likeService, departmentQueryService);
    }

    @Test
    @DisplayName("OfficialFeedController listOfficialAnnouncements should use DepartmentQueryService")
    void testOfficialFeedUsesDepartmentQueryService() {
        when(postService.getApprovedOfficialPosts(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        Department dept = Department.builder().id(1L).name("Khoa CNTT").code("CNTT").isActive(true).build();
        when(departmentQueryService.getActiveDepartments()).thenReturn(List.of(dept));

        Model model = new ExtendedModelMap();
        String view = officialFeedController.listOfficialAnnouncements(null, null, 0, model);

        assertEquals("feed/official-list", view);
        assertTrue(model.containsAttribute("departments"));
        verify(departmentQueryService, times(1)).getActiveDepartments();
    }

    @Test
    @DisplayName("OfficialFeedController downloadAttachment should query PostService.getAttachmentById")
    void testDownloadAttachmentUsesPostService() {
        when(postService.getAttachmentById(99L)).thenReturn(null);

        officialFeedController.downloadAttachment(99L, request, response);

        verify(postService, times(1)).getAttachmentById(99L);
        verify(response, times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("ForumController viewStudentForum should use DepartmentQueryService")
    void testForumUsesDepartmentQueryService() {
        when(postService.searchApprovedForumPostsDto(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        Department dept = Department.builder().id(1L).name("Khoa CNTT").code("CNTT").isActive(true).build();
        when(departmentQueryService.getActiveDepartments()).thenReturn(List.of(dept));

        Model model = new ExtendedModelMap();
        String view = forumController.viewStudentForum("all", null, null, 0, request, model);

        assertEquals("feed/forum", view);
        assertTrue(model.containsAttribute("departments"));
        verify(departmentQueryService, times(1)).getActiveDepartments();
    }
}

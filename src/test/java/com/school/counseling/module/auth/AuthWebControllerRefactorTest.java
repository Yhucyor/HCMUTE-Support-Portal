package com.school.counseling.module.auth;

import com.school.counseling.module.auth.controller.AuthWebController;
import com.school.counseling.module.auth.controller.ProfileWebController;
import com.school.counseling.module.auth.dto.UserProfileDto;
import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.service.AuthService;
import com.school.counseling.module.auth.service.DepartmentQueryService;
import com.school.counseling.module.auth.service.ProfileService;
import com.school.counseling.module.academic.service.HcmuteCrawlerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthWebControllerRefactorTest {

    @Mock
    private AuthService authService;

    @Mock
    private DepartmentQueryService departmentQueryService;

    @Mock
    private ProfileService profileService;

    @Mock
    private HcmuteCrawlerService crawlerService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    private AuthWebController authWebController;
    private ProfileWebController profileWebController;

    @BeforeEach
    void setUp() {
        authWebController = new AuthWebController(authService, departmentQueryService);
        profileWebController = new ProfileWebController(profileService, departmentQueryService, crawlerService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AuthWebController registerPage should load active departments from DepartmentQueryService cache")
    void testRegisterPageLoadsDepartmentsFromQueryService() {
        when(request.getSession(true)).thenReturn(session);
        Department dept1 = Department.builder().id(1L).name("Khoa CNTT").code("CNTT").isActive(true).build();
        Department dept2 = Department.builder().id(2L).name("Khoa Cơ khí").code("CK").isActive(true).build();
        when(departmentQueryService.getActiveDepartments()).thenReturn(List.of(dept1, dept2));

        Model model = new ExtendedModelMap();
        String view = authWebController.registerPage(request, model);

        assertEquals("auth/register", view);
        assertTrue(model.containsAttribute("departments"));
        @SuppressWarnings("unchecked")
        List<Department> result = (List<Department>) model.getAttribute("departments");
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(departmentQueryService, times(1)).getActiveDepartments();
    }

    @Test
    @DisplayName("ProfileWebController viewProfile should load active departments from DepartmentQueryService cache")
    void testProfileViewLoadsDepartmentsFromQueryService() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(100L)
                .username("student01")
                .role("ROLE_STUDENT")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        UserProfileDto profileDto = UserProfileDto.builder()
                .id(100L)
                .username("student01")
                .fullName("Nguyen Van A")
                .email("student01@hcmute.edu.vn")
                .build();
        when(profileService.getProfile(100L)).thenReturn(profileDto);

        Department dept = Department.builder().id(1L).name("Khoa CNTT").code("CNTT").isActive(true).build();
        when(departmentQueryService.getActiveDepartments()).thenReturn(List.of(dept));

        Model model = new ExtendedModelMap();
        String view = profileWebController.viewProfile(model);

        assertEquals("auth/profile", view);
        assertTrue(model.containsAttribute("departments"));
        verify(departmentQueryService, times(1)).getActiveDepartments();
    }
}

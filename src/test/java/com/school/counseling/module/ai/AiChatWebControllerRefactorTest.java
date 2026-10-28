package com.school.counseling.module.ai;

import com.school.counseling.module.ai.controller.AiChatWebController;
import com.school.counseling.module.ai.service.AiConversationService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.auth.service.DepartmentQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatWebControllerRefactorTest {

    @Mock
    private AiConversationService conversationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentQueryService departmentQueryService;

    private AiChatWebController controller;

    @BeforeEach
    void setUp() {
        controller = new AiChatWebController(conversationService, userRepository, departmentQueryService);
    }

    @Test
    @DisplayName("AiChatWebController chatPage should load active departments from DepartmentQueryService")
    void testChatPageLoadsDepartmentsFromQueryService() {
        Department dept = Department.builder().id(1L).name("Phòng Đào tạo").code("PDT").isActive(true).build();
        when(departmentQueryService.getActiveDepartments()).thenReturn(List.of(dept));

        Model model = new ExtendedModelMap();
        String view = controller.chatPage(model);

        assertEquals("ai/ai-chat", view);
        assertTrue(model.containsAttribute("departments"));
        @SuppressWarnings("unchecked")
        List<Department> depts = (List<Department>) model.getAttribute("departments");
        assertNotNull(depts);
        assertEquals(1, depts.size());
        verify(departmentQueryService, times(1)).getActiveDepartments();
    }
}

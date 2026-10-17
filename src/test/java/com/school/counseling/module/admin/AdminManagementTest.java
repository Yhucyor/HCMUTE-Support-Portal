package com.school.counseling.module.admin;

import com.school.counseling.module.admin.dto.InternalMessageDto;
import com.school.counseling.module.admin.dto.SaveDepartmentDto;
import com.school.counseling.module.admin.dto.SendInternalMessageRequest;
import com.school.counseling.module.admin.dto.UserManagementDto;
import com.school.counseling.module.admin.entity.InternalStaffAdminMessage;
import com.school.counseling.module.admin.repository.InternalStaffAdminMessageRepository;
import com.school.counseling.module.admin.service.AdminDepartmentService;
import com.school.counseling.module.admin.service.AdminUserService;
import com.school.counseling.module.admin.service.InternalMessageService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.Role;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.UserStatus;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.RoleRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.ticket.repository.TicketRepository;
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
 * UNIT TEST TDD: KIỂM THỬ QUẢN TRỊ ADMIN (USER, DEPARTMENT, INTERNAL MESSAGE)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TDD-ADM-01: Kiểm thử Module Quản trị Admin & Điều phối Nội bộ")
class AdminManagementTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private InternalStaffAdminMessageRepository messageRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminDepartmentService adminDepartmentService;

    @InjectMocks
    private InternalMessageService internalMessageService;

    private User sampleUser;
    private Department sampleDept;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        studentRole = Role.builder().id(1L).name("ROLE_STUDENT").description("Sinh viên").build();
        sampleDept = Department.builder().id(10L).name("Phòng Đào tạo").code("DAO_TAO").isActive(true).build();

        sampleUser = User.builder()
                .id(100L)
                .username("student_test")
                .fullName("Nguyễn Văn Test")
                .email("test@hcmute.edu.vn")
                .role(studentRole)
                .department(sampleDept)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("1. AdminUserService.toggleUserLock: Khóa tài khoản đang hoạt động")
    void testToggleUserLock_LockActiveUser() {
        when(userRepository.findByIdWithRoleAndDepartment(100L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserManagementDto result = adminUserService.toggleUserLock(100L);

        assertEquals("LOCKED", result.getStatus());
        assertEquals(UserStatus.LOCKED, sampleUser.getStatus());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("2. AdminDepartmentService.toggleDepartmentStatus: Bật/Tắt trạng thái hoạt động Đơn vị")
    void testToggleDepartmentStatus() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(sampleDept));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.countByDepartmentId(10L)).thenReturn(5L);
        when(ticketRepository.countByDepartmentId(10L)).thenReturn(20L);

        var result = adminDepartmentService.toggleDepartmentStatus(10L);

        assertFalse(result.getIsActive());
        assertFalse(sampleDept.getIsActive());
        verify(departmentRepository, times(1)).save(sampleDept);
    }

    @Test
    @DisplayName("3. InternalMessageService.sendMessage: Cán bộ gửi tin nhắn điều phối nội bộ thành công")
    void testSendInternalMessage_Success() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(sampleUser));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(sampleDept));

        when(messageRepository.save(any(InternalStaffAdminMessage.class))).thenAnswer(invocation -> {
            InternalStaffAdminMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        SendInternalMessageRequest request = SendInternalMessageRequest.builder()
                .departmentId(10L)
                .subject("Hỗ trợ kỹ thuật khẩn cấp")
                .content("Cần hỗ trợ reset mật khẩu cho thí sinh")
                .messageType("INQUIRY")
                .build();

        InternalMessageDto result = internalMessageService.sendMessage(100L, request);

        assertNotNull(result);
        assertEquals("Hỗ trợ kỹ thuật khẩn cấp", result.getSubject());
        assertEquals("Cần hỗ trợ reset mật khẩu cho thí sinh", result.getContent());
        assertFalse(result.getIsRead());
        verify(messageRepository, times(1)).save(any(InternalStaffAdminMessage.class));
    }
}

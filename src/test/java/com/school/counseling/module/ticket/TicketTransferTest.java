package com.school.counseling.module.ticket;

import com.school.counseling.common.exception.AccessDeniedBusinessException;
import com.school.counseling.common.storage.IStorageService;
import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.ticket.dto.TicketTransferRequest;
import com.school.counseling.module.ticket.dto.TicketResponseDto;
import com.school.counseling.module.ticket.entity.Ticket;
import com.school.counseling.module.ticket.entity.TicketTransferHistory;
import com.school.counseling.module.ticket.event.TicketTransferredEvent;
import com.school.counseling.module.ticket.repository.AttachmentRepository;
import com.school.counseling.module.ticket.repository.TicketHistoryRepository;
import com.school.counseling.module.ticket.repository.TicketRepository;
import com.school.counseling.module.ticket.repository.TicketTransferHistoryRepository;
import com.school.counseling.module.ticket.service.SlaCalculatorService;
import com.school.counseling.module.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UNIT TEST - MODULE TICKET: Kiểm thử luồng chuyển giao vé (Ticket Transfer) và bảo mật đơn vị")
class TicketTransferTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketHistoryRepository ticketHistoryRepository;

    @Mock
    private TicketTransferHistoryRepository ticketTransferHistoryRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private SlaCalculatorService slaCalculatorService;

    @Mock
    private IStorageService storageService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TicketService ticketService;

    private Department deptDaoTao;
    private Department deptCTSV;
    private User advisorDaoTao;
    private UserPrincipal principalDaoTao;
    private Ticket sampleTicket;

    @BeforeEach
    void setUp() {
        deptDaoTao = Department.builder().id(1L).name("Phòng Đào tạo").code("PDT").isActive(true).build();
        deptCTSV = Department.builder().id(2L).name("Phòng Công tác Sinh viên").code("CTSV").isActive(true).build();

        advisorDaoTao = User.builder()
                .id(10L)
                .username("advisor_daotao")
                .fullName("Cán bộ Đào tạo")
                .department(deptDaoTao)
                .build();

        principalDaoTao = UserPrincipal.builder()
                .id(10L)
                .username("advisor_daotao")
                .departmentId(1L)
                .role("ROLE_STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_STAFF")))
                .build();

        sampleTicket = Ticket.builder()
                .id(100L)
                .ticketCode("TK-20261025-001")
                .title("Hỏi về thủ tục phúc khảo điểm")
                .status("IN_PROGRESS")
                .department(deptDaoTao)
                .assignedTo(advisorDaoTao)
                .build();

        when(ticketRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(sampleTicket));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(deptCTSV));
        when(userRepository.findById(10L)).thenReturn(Optional.of(advisorDaoTao));
        when(ticketTransferHistoryRepository.findByTicketIdWithRelations(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("Chuyển giao vé thành công: Đổi phòng ban, reset assignedTo về null, đưa status về OPEN và phát sự kiện")
    void testTransferTicket_Success() {
        TicketTransferRequest request = new TicketTransferRequest();
        request.setToDepartmentId(2L);
        request.setReason("Nội dung thuộc phạm vi quản lý của Phòng CTSV");

        TicketResponseDto result = ticketService.transferTicket(100L, request, principalDaoTao);

        assertNotNull(result);
        assertEquals("OPEN", sampleTicket.getStatus());
        assertNull(sampleTicket.getAssignedTo());
        assertEquals(deptCTSV, sampleTicket.getDepartment());

        verify(ticketTransferHistoryRepository, times(1)).save(any(TicketTransferHistory.class));
        verify(ticketRepository, times(1)).save(sampleTicket);

        ArgumentCaptor<TicketTransferredEvent> eventCaptor = ArgumentCaptor.forClass(TicketTransferredEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        TicketTransferredEvent published = eventCaptor.getValue();
        assertEquals("TK-20261025-001", published.ticketCode());
        assertEquals("Phòng Đào tạo", published.fromDeptName());
        assertEquals("Phòng Công tác Sinh viên", published.toDeptName());
    }

    @Test
    @DisplayName("Chuyển giao vé thất bại: Cán bộ không thuộc đơn vị hiện tại của vé cố tình chuyển")
    void testTransferTicket_ForbiddenDepartment_ThrowsException() {
        UserPrincipal foreignPrincipal = UserPrincipal.builder()
                .id(20L)
                .username("advisor_khoa")
                .departmentId(99L) // Không phải phòng Đào tạo (1L)
                .role("ROLE_STAFF")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_STAFF")))
                .build();

        TicketTransferRequest request = new TicketTransferRequest();
        request.setToDepartmentId(2L);
        request.setReason("Lý do trái quyền");

        assertThrows(AccessDeniedBusinessException.class, () ->
                ticketService.transferTicket(100L, request, foreignPrincipal));
    }

    @Test
    @DisplayName("Chuyển giao vé thất bại: Đơn vị tiếp nhận trùng với đơn vị hiện tại")
    void testTransferTicket_SameDepartment_ThrowsException() {
        TicketTransferRequest request = new TicketTransferRequest();
        request.setToDepartmentId(1L); // Trùng với deptDaoTao (1L)
        request.setReason("Chuyển lòng vòng");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ticketService.transferTicket(100L, request, principalDaoTao));
        assertTrue(ex.getMessage().contains("phải khác với đơn vị hiện tại"));
    }

    @Test
    @DisplayName("Chuyển giao vé thất bại: Vé đã đóng (CLOSED) không được phép chuyển tiếp")
    void testTransferTicket_TicketClosed_ThrowsException() {
        sampleTicket.setStatus("CLOSED");

        TicketTransferRequest request = new TicketTransferRequest();
        request.setToDepartmentId(2L);
        request.setReason("Cố chuyển vé đã đóng");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ticketService.transferTicket(100L, request, principalDaoTao));
        assertTrue(ex.getMessage().contains("đã đóng hoặc đã hủy"));
    }
}

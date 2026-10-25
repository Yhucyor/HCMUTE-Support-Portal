package com.school.counseling.module.ticket;

import com.school.counseling.common.storage.IStorageService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.ticket.dto.CreateTicketRequest;
import com.school.counseling.module.ticket.dto.TicketResponseDto;
import com.school.counseling.module.ticket.entity.Ticket;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UNIT TEST - MODULE TICKET: Tạo Ticket (Sinh viên đã đăng nhập & Khách vãng lai)")
class TicketCreationTest {

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

    private Department activeDept;
    private Department inactiveDept;
    private User sampleStudent;

    @BeforeEach
    void setUp() {
        activeDept = Department.builder().id(1L).name("Phòng Đào tạo").code("PDT").isActive(true).build();
        inactiveDept = Department.builder().id(2L).name("Khoa Cũ").code("OLD").isActive(false).build();

        sampleStudent = User.builder()
                .id(10L)
                .username("student10")
                .fullName("Nguyen Van Sinh Vien")
                .email("student10@hcmute.edu.vn")
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(activeDept));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(inactiveDept));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleStudent));

        when(slaCalculatorService.resolvePriority(anyString())).thenReturn("HIGH");
        when(slaCalculatorService.calculateDueDateByCategory(anyString())).thenReturn(LocalDateTime.now().plusHours(48));

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setId(1001L);
            t.setTicketCode("TK-20261025-001");
            return t;
        });
        when(ticketTransferHistoryRepository.findByTicketIdWithRelations(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("Sinh viên đăng nhập tạo Ticket thành công: Tự gán creator, tính SLA và lưu lịch sử")
    void testCreateTicket_ByAuthenticatedStudent_Success() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setDepartmentId(1L);
        request.setTopicCategory("LMS_PORTAL_LOCK");
        request.setTitle("Không đăng nhập được LMS kiểm tra giữa kỳ");
        request.setDescription("Em nhập đúng mật khẩu nhưng hệ thống báo lỗi");

        TicketResponseDto result = ticketService.createTicket(request, 10L, Collections.emptyList());

        assertNotNull(result);
        assertEquals(1001L, result.getId());
        assertEquals("TK-20261025-001", result.getTicketCode());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(ticketHistoryRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Khách vãng lai tạo Ticket thành công: Bắt buộc email và tự sinh guestAccessToken")
    void testCreateTicket_ByGuest_GeneratesGuestToken() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setDepartmentId(1L);
        request.setTopicCategory("GENERAL_INQUIRY");
        request.setTitle("Hỏi thông tin tuyển sinh liên thông");
        request.setDescription("Em muốn hỏi thủ tục nộp hồ sơ");
        request.setGuestEmail("guest@gmail.com");
        request.setGuestName("Nguyen Van Khach");

        TicketResponseDto result = ticketService.createTicket(request, null, Collections.emptyList());

        assertNotNull(result);
        verify(ticketRepository, times(1)).save(argThat(ticket ->
                "guest@gmail.com".equals(ticket.getGuestEmail())
                        && ticket.getGuestToken() != null
                        && !ticket.getGuestToken().isBlank()
        ));
    }

    @Test
    @DisplayName("Khách vãng lai thiếu email: Báo lỗi nghiệp vụ rõ ràng")
    void testCreateTicket_ByGuest_MissingEmail_ThrowsException() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setDepartmentId(1L);
        request.setTopicCategory("GENERAL_INQUIRY");
        request.setTitle("Hỏi thông tin");
        request.setGuestEmail("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ticketService.createTicket(request, null, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("Vui lòng nhập địa chỉ email hợp lệ"));
    }

    @Test
    @DisplayName("Chặn tạo Ticket vào phòng ban/đơn vị đang tạm dừng hoạt động (isActive = false)")
    void testCreateTicket_InactiveDepartment_ThrowsException() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setDepartmentId(2L); // inactiveDept
        request.setTitle("Yêu cầu gửi đơn vị đóng");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ticketService.createTicket(request, 10L, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("tạm dừng tiếp nhận"));
    }
}

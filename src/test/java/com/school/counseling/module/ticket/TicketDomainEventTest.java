package com.school.counseling.module.ticket;

import com.school.counseling.common.storage.IStorageService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.ticket.repository.AttachmentRepository;
import com.school.counseling.module.ticket.entity.Ticket;
import com.school.counseling.module.ticket.event.TicketClaimedEvent;
import com.school.counseling.module.ticket.repository.TicketHistoryRepository;
import com.school.counseling.module.ticket.repository.TicketRepository;
import com.school.counseling.module.ticket.service.SlaCalculatorService;
import com.school.counseling.module.ticket.service.TicketService;
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

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UNIT TEST TDD: KIỂM THỬ SỰ KIỆN MIỀN (DOMAIN EVENTS) & ATOMIC CLAIMING
 * 
 * Kiểm tra xem khi Cán bộ tiếp nhận Ticket:
 * 1. Phương thức claimTicketAtomic() được gọi để khoá hàng cập nhật nguyên tử.
 * 2. ApplicationEventPublisher phát sự kiện TicketClaimedEvent đúng 1 lần (để gửi mail bất đồng bộ không phong tỏa luồng chính).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TDD-TCK-02: Kiểm thử Domain Event Publishing & Atomic Claiming")
class TicketDomainEventTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TicketHistoryRepository ticketHistoryRepository;

    @Mock
    private com.school.counseling.module.ticket.repository.TicketTransferHistoryRepository ticketTransferHistoryRepository;

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

    @Test
    @DisplayName("Khi Cán bộ Claim vé thành công -> Bắt buộc publish TicketClaimedEvent đúng 1 lần")
    void testClaimTicket_PublishesTicketClaimedEvent() {
        Long ticketId = 100L;
        Long staffId = 200L;

        Department dept = Department.builder().id(1L).code("CNTT").name("Khoa CNTT").build();
        User staff = User.builder().id(staffId).fullName("Thầy Hoàng").email("hoang@hcmute.edu.vn").department(dept).build();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ticketCode("TK-12345")
                .title("Lỗi đóng học phí")
                .guestEmail("guest@gmail.com")
                .guestName("Nguyễn Văn A")
                .department(dept)
                .status("OPEN")
                .build();

        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.claimTicketAtomic(ticketId, staff)).thenReturn(1); // Cập nhật nguyên tử thành công
        when(ticketRepository.findByIdWithRelations(ticketId)).thenReturn(Optional.of(ticket));
        when(attachmentRepository.findByTicketId(any())).thenReturn(Collections.emptyList());

        ticketService.claimTicket(ticketId, staffId);

        // Bắt và kiểm tra sự kiện Domain Event đã phát
        ArgumentCaptor<TicketClaimedEvent> eventCaptor = ArgumentCaptor.forClass(TicketClaimedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        TicketClaimedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(ticketId, publishedEvent.ticketId());
        assertEquals("guest@gmail.com", publishedEvent.recipientEmail());
        assertEquals("Thầy Hoàng", publishedEvent.staffName());
        assertEquals("Khoa CNTT", publishedEvent.departmentName());
    }
}

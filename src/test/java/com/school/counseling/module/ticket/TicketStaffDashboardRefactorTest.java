package com.school.counseling.module.ticket;

import com.school.counseling.module.admin.dto.DepartmentAdminDto;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.ticket.dto.StaffDashboardDataDto;
import com.school.counseling.module.ticket.dto.TicketSummaryDto;
import com.school.counseling.module.ticket.repository.TicketRepository;
import com.school.counseling.module.ticket.service.SlaCalculatorService;
import com.school.counseling.module.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketStaffDashboardRefactorTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SlaCalculatorService slaCalculatorService;

    @InjectMocks
    private TicketService ticketService;

    @Test
    @DisplayName("Kiểm tra getActiveDepartments trả về danh sách DTO chuẩn, không leak entity")
    void testGetActiveDepartments_ShouldReturnDtoList() {
        Department dept = Department.builder()
                .id(1L)
                .name("Khoa CNTT")
                .code("KHOA_CNTT")
                .officeLocation("E1-402")
                .isActive(true)
                .build();

        when(departmentRepository.findByIsActiveTrue()).thenReturn(List.of(dept));

        List<DepartmentAdminDto> result = ticketService.getActiveDepartments();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Khoa CNTT", result.get(0).getName());
        assertEquals("KHOA_CNTT", result.get(0).getCode());
    }

    @Test
    @DisplayName("Kiểm tra getStaffDashboardData tính đúng số lượng trạng thái và phân trang")
    void testGetStaffDashboardData_ShouldComputeCountsAndPaging() {
        Long deptId = 2L;
        TicketSummaryDto t1 = TicketSummaryDto.builder()
                .id(101L)
                .ticketCode("TK-001")
                .status("OPEN")
                .hasStudentReply(false)
                .dueDate(LocalDateTime.now().plusDays(2))
                .build();

        TicketSummaryDto t2 = TicketSummaryDto.builder()
                .id(102L)
                .ticketCode("TK-002")
                .status("IN_PROGRESS")
                .hasStudentReply(true)
                .dueDate(LocalDateTime.now().minusHours(5)) // Quá hạn
                .build();

        TicketSummaryDto t3 = TicketSummaryDto.builder()
                .id(103L)
                .ticketCode("TK-003")
                .status("RESOLVED")
                .hasStudentReply(false)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();

        when(ticketRepository.findSummaryByDepartmentId(deptId)).thenReturn(List.of(t1, t2, t3));

        StaffDashboardDataDto result = ticketService.getStaffDashboardData(deptId, "ALL", 0, 10);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getCountOpen());
        assertEquals(1, result.getCountInProgress());
        assertEquals(1, result.getCountResolved());
        assertEquals(1, result.getCountNewReplies());
        assertEquals(3, result.getTickets().size());
    }
}

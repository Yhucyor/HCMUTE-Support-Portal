package com.school.counseling.module.admin;

import com.school.counseling.module.admin.dto.AdminSlaDashboardDto;
import com.school.counseling.module.admin.service.AdminDashboardService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.ticket.dto.TicketSummaryDto;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UNIT TEST - MODULE ADMIN: Thống kê Dashboard SLA & Hiệu suất phòng ban")
class AdminDashboardServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    private Department deptDaoTao;

    @BeforeEach
    void setUp() {
        deptDaoTao = Department.builder().id(1L).name("Phòng Đào tạo").code("DAO_TAO").isActive(true).build();
        when(departmentRepository.findAll()).thenReturn(List.of(deptDaoTao));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(deptDaoTao));
        when(userRepository.countByDepartmentId(1L)).thenReturn(5L);
    }

    @Test
    @DisplayName("Dashboard khi hệ thống chưa có Ticket nào (0 tickets): Không chia cho 0, complianceRate mặc định 100%")
    void testGetSlaDashboardStats_EmptyData() {
        when(ticketRepository.getOverallSlaAggregate(any(), any())).thenReturn(Collections.emptyList());
        when(ticketRepository.countByPriorityFiltered(any(), any())).thenReturn(Collections.emptyList());
        when(ticketRepository.getDepartmentSlaAggregates(any())).thenReturn(Collections.emptyList());
        when(ticketRepository.findTopOverdueTicketsFiltered(any())).thenReturn(Collections.emptyList());

        AdminSlaDashboardDto stats = adminDashboardService.getSlaDashboardStats("ALL", null);

        assertNotNull(stats);
        assertEquals(0L, stats.getTotalTickets());
        assertEquals(100.0, stats.getSlaComplianceRate());
        assertNull(stats.getAverageRating());
        assertEquals(1, stats.getDepartmentStats().size());
        assertEquals(0L, stats.getDepartmentStats().get(0).getTotalTickets());
        assertEquals(5L, stats.getDepartmentStats().get(0).getStaffCount());
    }

    @Test
    @DisplayName("Dashboard khi có dữ liệu: Tính toán chuẩn tỷ lệ SLA Compliance và trung bình đánh giá sao")
    void testGetSlaDashboardStats_WithData() {
        // Mock row: [total, open, inProgress, resolved, closed, overdue, onTime, avgRating]
        Object[] overallRow = new Object[]{10L, 2L, 3L, 4L, 1L, 2L, 4L, 4.67};
        when(ticketRepository.getOverallSlaAggregate(any(), any())).thenReturn(List.<Object[]>of(overallRow));

        // Mock priority rows: [priority, count]
        List<Object[]> priorityRows = List.<Object[]>of(
                new Object[]{"URGENT", 3L},
                new Object[]{"HIGH", 7L}
        );
        when(ticketRepository.countByPriorityFiltered(any(), any())).thenReturn(priorityRows);

        // Mock department SLA aggregate: [deptId, deptName, deptCode, total, open, inProg, resolved, overdue, avgRating]
        List<Object[]> deptRows = List.<Object[]>of(
                new Object[]{1L, "Phòng Đào tạo", "DAO_TAO", 10L, 2L, 3L, 5L, 2L, 4.67}
        );
        when(ticketRepository.getDepartmentSlaAggregates(any())).thenReturn(deptRows);

        AdminSlaDashboardDto stats = adminDashboardService.getSlaDashboardStats("7_DAYS", 1L);

        assertNotNull(stats);
        assertEquals(10L, stats.getTotalTickets());
        assertEquals(5L, stats.getTotalResolved()); // resolved (4) + closed (1)
        assertEquals(4L, stats.getTotalOnTime());
        // Compliance rate: 4 onTime / 5 totalResolved * 100 = 80.0%
        assertEquals(80.0, stats.getSlaComplianceRate());
        assertEquals(4.7, stats.getAverageRating()); // Làm tròn 1 chữ số thập phân

        assertEquals(3L, stats.getPriorityStats().get("URGENT"));
        assertEquals(7L, stats.getPriorityStats().get("HIGH"));
        assertEquals(0L, stats.getPriorityStats().get("MEDIUM"));
        assertEquals("Phòng Đào tạo", stats.getSelectedDepartmentName());
    }
}

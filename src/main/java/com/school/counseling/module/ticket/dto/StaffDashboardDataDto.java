package com.school.counseling.module.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO đóng gói toàn bộ dữ liệu thống kê và phân trang cho Bàn làm việc Cán bộ (Staff Dashboard)
 * Chuẩn hóa MVC: Tách toàn bộ logic lọc và phân trang ra khỏi Web Controller.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDashboardDataDto {
    private List<TicketSummaryDto> tickets;
    private Page<TicketSummaryDto> ticketPage;
    private int currentPage;
    private int totalPages;
    private int totalElements;
    private long countOpen;
    private long countInProgress;
    private long countOverdue;
    private long countResolved;
    private long countNewReplies;
}

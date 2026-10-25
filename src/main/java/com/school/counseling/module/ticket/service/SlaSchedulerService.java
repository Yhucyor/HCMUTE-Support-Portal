package com.school.counseling.module.ticket.service;

import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.notification.service.EmailAsyncService;
import com.school.counseling.module.ticket.entity.Ticket;
import com.school.counseling.module.ticket.entity.TicketHistory;
import com.school.counseling.module.ticket.repository.TicketHistoryRepository;
import com.school.counseling.module.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ĐỘNG CƠ GIÁM SÁT & TỰ ĐỘNG HÓA SLA (SLA Engine Scheduler)
 * 
 * Chức năng chính:
 * 1. Tự động phát hiện Ticket quá hạn SLA (now > due_date) -> Chuyển sang OVERDUE & gửi email cảnh báo.
 *    - Nếu vé OPEN chưa ai nhận: Tự động chuyển quyền quản lý lên Admin và gia hạn thêm 24 giờ.
 *    - Nếu vé đã có Cán bộ nhận: Gửi email cảnh báo vi phạm SLA tới Cán bộ và Trưởng đơn vị.
 * 2. Tự động hoàn tất (CLOSED) các vé ở trạng thái RESOLVED sau 72 giờ nếu sinh viên không có khiếu nại.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaSchedulerService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final EmailAsyncService emailAsyncService;
    private final com.school.counseling.module.auth.repository.UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    /**
     * Tự động quét và phát hiện các Ticket quá hạn SLA.
     * Tần suất mặc định: Mỗi 15 phút (Cron expression cấu hình trong application.yml hoặc fallback 0 * /15 * * * *)
     */
    @Scheduled(cron = "${app.sla.overdue-scan-cron:0 */15 * * * *}")
    @Transactional
    public int scanAndMarkOverdueTickets() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> overdueTickets = ticketRepository.findOverdueTicketsWithRelations(now);

        if (overdueTickets.isEmpty()) {
            log.debug("SLA Scheduler: Không có ticket nào bị vi phạm hạn chót SLA tại thời điểm {}", now);
            return 0;
        }

        log.warn("SLA Scheduler: Phát hiện {} ticket vi phạm hạn chót SLA! Tiến hành chuyển trạng thái OVERDUE...", overdueTickets.size());

        int count = 0;
        for (Ticket ticket : overdueTickets) {
            try {
                String oldStatus = ticket.getStatus();
                ticket.setStatus("OVERDUE");

                Department dept = ticket.getDepartment();
                String deptName = (dept != null) ? dept.getName() : "Khoa / Phòng ban phụ trách";
                String dueStr = (ticket.getDueDate() != null) ? ticket.getDueDate().format(DATE_FORMATTER) : "Không xác định";

                String actionNote;
                if (ticket.getAssignedTo() == null) {
                    // [USER_REQUEST]: Vé chưa có người nhận -> Gán mặc định người phụ trách về cho Admin, KHÔNG gia hạn thêm giờ
                    User defaultAdmin = userRepository.findByUsername("admin")
                            .orElseGet(() -> userRepository.findAll().stream()
                                    .filter(u -> u.getRole() != null && "ROLE_ADMIN".equalsIgnoreCase(u.getRole().getName()))
                                    .findFirst().orElse(null));

                    if (defaultAdmin != null) {
                        ticket.setAssignedTo(defaultAdmin);
                    }
                    // Giữ nguyên hạn chót (dueDate), không cộng thêm thời gian tiếp nhận

                    actionNote = String.format("Yêu cầu quá hạn tiếp nhận tại đơn vị [%s]. Hệ thống chuyển sang trạng thái QUÁ HẠN (OVERDUE) và phân bổ cho Quản trị viên (Admin) trực tiếp giải quyết.", deptName);
                    log.warn("SLA Scheduler: Ticket #{} quá hạn tiếp nhận tại {}. Đã chuyển Admin ({}) xử lý ở trạng thái OVERDUE.",
                            ticket.getTicketCode(), deptName, defaultAdmin != null ? defaultAdmin.getFullName() : "Admin");
                } else {
                    // [USER_REQUEST]: Vé đã có Cán bộ (Staff) tiếp nhận nhưng xử lý quá hạn -> Giữ nguyên Cán bộ phụ trách (Staff) và báo OVERDUE
                    String staffName = ticket.getAssignedTo().getFullName();
                    actionNote = String.format("Cán bộ [%s] thuộc đơn vị [%s] xử lý quá thời hạn cam kết (Hạn chót: %s). Hệ thống ghi nhận trạng thái QUÁ HẠN (OVERDUE) chờ giải quyết khẩn cấp.", staffName, deptName, dueStr);
                    log.warn("SLA Scheduler: Ticket #{} quá hạn xử lý bởi {}. Đã đánh dấu OVERDUE.", ticket.getTicketCode(), staffName);
                }

                // 1. Ghi nhận nhật ký lịch sử hệ thống (Audit Trail)
                TicketHistory history = TicketHistory.builder()
                        .ticket(ticket)
                        .actor(null)
                        .actorName("HỆ THỐNG (SLA DETECTOR)")
                        .fromStatus(oldStatus)
                        .toStatus("OVERDUE")
                        .actionNote(actionNote)
                        .build();

                ticketRepository.save(ticket);
                ticketHistoryRepository.save(history);

                // 2. Gửi email cảnh báo vi phạm SLA bất đồng bộ
                sendOverdueNotification(ticket);

                count++;
                log.info("SLA Scheduler: Ticket #{} (ID: {}) đã đổi sang OVERDUE thành công.", ticket.getTicketCode(), ticket.getId());
            } catch (Exception e) {
                log.error("SLA Scheduler: Lỗi khi xử lý quá hạn cho ticket ID {}: {}", ticket.getId(), e.getMessage(), e);
            }
        }

        return count;
    }

    /**
     * Tự động đóng (CLOSED) các Ticket ở trạng thái RESOLVED sau 72 giờ không có khiếu nại.
     * Tần suất mặc định: Mỗi 30 phút.
     */
    @Scheduled(cron = "${app.sla.auto-close-cron:0 */30 * * * *}")
    @Transactional
    public int autoCloseResolvedTickets() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(72);
        List<Ticket> resolvedTickets = ticketRepository.findResolvedTicketsEligibleForAutoClose(threshold);

        if (resolvedTickets.isEmpty()) {
            log.debug("SLA Scheduler: Không có ticket RESOLVED nào vượt quá 72 giờ tại thời điểm kiểm tra.");
            return 0;
        }

        log.info("SLA Scheduler: Phát hiện {} ticket đã giải quyết quá 72 giờ không khiếu nại. Tiến hành tự động đóng...", resolvedTickets.size());

        int count = 0;
        for (Ticket ticket : resolvedTickets) {
            try {
                ticket.setStatus("CLOSED");
                ticket.setClosedAt(LocalDateTime.now());

                // Mặc định gán đánh giá hài lòng 5 sao nếu sinh viên không khiếu nại gì sau 72h
                if (ticket.getRating() == null) {
                    ticket.setRating(5);
                }

                TicketHistory history = TicketHistory.builder()
                        .ticket(ticket)
                        .actor(null)
                        .actorName("HỆ THỐNG (SLA AUTO-CLOSE)")
                        .fromStatus("RESOLVED")
                        .toStatus("CLOSED")
                        .actionNote("Yêu cầu đã được giải quyết quá 72 giờ và sinh viên không có khiếu nại bổ sung. Hệ thống tự động hoàn tất và đóng yêu cầu (CLOSED).")
                        .build();

                ticketRepository.save(ticket);
                ticketHistoryRepository.save(history);

                count++;
                log.info("SLA Scheduler: Đã tự động đóng ticket #{} (ID: {}) thành công.", ticket.getTicketCode(), ticket.getId());
            } catch (Exception e) {
                log.error("SLA Scheduler: Lỗi khi tự động đóng ticket ID {}: {}", ticket.getId(), e.getMessage(), e);
            }
        }

        return count;
    }

    /**
     * Xác định địa chỉ người nhận và gửi email cảnh báo SLA qua EmailAsyncService
     */
    private void sendOverdueNotification(Ticket ticket) {
        try {
            String toEmail = null;
            String recipientName = null;
            Department dept = ticket.getDepartment();
            String deptName = (dept != null) ? dept.getName() : "Khoa / Phòng ban phụ trách";

            // Nếu đã có Cán bộ tiếp nhận -> gửi trực tiếp cho Cán bộ đó
            if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getEmail() != null) {
                User staff = ticket.getAssignedTo();
                toEmail = staff.getEmail();
                recipientName = staff.getFullName();
            } else if (dept != null && dept.getContactEmail() != null) {
                // Nếu chưa có Cán bộ tiếp nhận -> gửi tới email liên hệ của Khoa/Phòng
                toEmail = dept.getContactEmail();
                recipientName = "Ban Lãnh Đạo & Cán Bộ " + deptName;
            }

            if (toEmail != null && !toEmail.isBlank()) {
                emailAsyncService.sendSlaOverdueAlertEmail(
                        toEmail,
                        recipientName,
                        ticket.getTicketCode(),
                        ticket.getTitle(),
                        ticket.getDueDate(),
                        ticket.getPriority(),
                        deptName
                );
            }
        } catch (Exception e) {
            log.warn("SLA Scheduler: Không thể gửi email cảnh báo quá hạn cho ticket #{}: {}", ticket.getTicketCode(), e.getMessage());
        }
    }
}

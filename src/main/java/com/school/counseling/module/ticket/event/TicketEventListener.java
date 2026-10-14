package com.school.counseling.module.ticket.event;

import com.school.counseling.module.notification.service.EmailAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe và xử lý các sự kiện bất đồng bộ liên quan đến Ticket.
 * Chạy trên thread pool mailTaskExecutor để không bao giờ giữ luồng Tomcat.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventListener {

    private final EmailAsyncService emailAsyncService;

    @Async("mailTaskExecutor")
    @EventListener
    public void handleTicketCreated(TicketCreatedEvent event) {
        if (event.recipientEmail() != null && !event.recipientEmail().isBlank()) {
            log.info("[Event Bus] Nhận TicketCreatedEvent #{} -> Gửi email xác nhận tới {}",
                    event.ticketCode(), event.recipientEmail());
            emailAsyncService.sendTicketCreatedEmail(
                    event.recipientEmail(),
                    event.recipientName(),
                    event.ticketCode(),
                    event.title(),
                    event.priority(),
                    event.dueDate(),
                    event.trackingUrl()
            );
        }
    }

    @Async("mailTaskExecutor")
    @EventListener
    public void handleTicketClaimed(TicketClaimedEvent event) {
        if (event.recipientEmail() != null && !event.recipientEmail().isBlank()) {
            log.info("[Event Bus] Nhận TicketClaimedEvent #{} -> Gửi email thông báo cán bộ đã nhận tới {}",
                    event.ticketCode(), event.recipientEmail());
            emailAsyncService.sendTicketClaimedEmail(
                    event.recipientEmail(),
                    event.recipientName(),
                    event.ticketCode(),
                    event.staffName(),
                    event.departmentName(),
                    event.trackingUrl()
            );
        }
    }

    @Async("mailTaskExecutor")
    @EventListener
    public void handleTicketResolved(TicketResolvedEvent event) {
        if (event.recipientEmail() != null && !event.recipientEmail().isBlank()) {
            log.info("[Event Bus] Nhận TicketResolvedEvent #{} -> Gửi email kết quả giải đáp tới {}",
                    event.ticketCode(), event.recipientEmail());
            emailAsyncService.sendTicketResolvedEmail(
                    event.recipientEmail(),
                    event.recipientName(),
                    event.ticketCode(),
                    event.replyContent(),
                    event.trackingUrl()
            );
        }
    }
}

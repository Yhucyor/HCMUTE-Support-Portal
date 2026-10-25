package com.school.counseling.module.notification;

import com.school.counseling.module.notification.service.EmailAsyncService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UNIT TEST - MODULE NOTIFICATION: Gửi email bất đồng bộ & Khả năng chịu lỗi SMTP")
class EmailAsyncServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailAsyncService emailAsyncService;

    @BeforeEach
    void setUp() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Gửi email tạo Ticket thành công khi địa chỉ email hợp lệ")
    void testSendTicketCreatedEmail_Success() {
        assertDoesNotThrow(() ->
                emailAsyncService.sendTicketCreatedEmail(
                        "student@hcmute.edu.vn",
                        "Nguyen Van A",
                        "TK-1001",
                        "Hỏi lịch thi học kỳ 2",
                        "URGENT",
                        LocalDateTime.now().plusHours(24),
                        "https://counseling.hcmute.edu.vn/ticket/TK-1001"
                ));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Bỏ qua an toàn khi email người nhận rỗng hoặc null (Không gọi JavaMailSender)")
    void testSendEmail_NullOrEmptyRecipient_SkipsGracefully() {
        assertDoesNotThrow(() ->
                emailAsyncService.sendTicketCreatedEmail(
                        null,
                        "Nguyen Van A",
                        "TK-1001",
                        "Hỏi lịch thi",
                        "URGENT",
                        LocalDateTime.now(),
                        "#"
                ));

        assertDoesNotThrow(() ->
                emailAsyncService.sendOtpEmail(
                        "   ",
                        "Nguyen Van A",
                        "123456",
                        "kích hoạt"
                ));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Chịu lỗi SMTP: Khi mail server gặp sự cố ném exception, hàm bắt lỗi nội bộ và không văng exception ra ngoài")
    void testSendEmail_SmtpFailure_CatchesGracefully() {
        doThrow(new MailSendException("SMTP connection refused: port 587"))
                .when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailAsyncService.sendOtpEmail(
                        "student@hcmute.edu.vn",
                        "Nguyen Van A",
                        "654321",
                        "đặt lại mật khẩu"
                ));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}

package com.school.counseling.module.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAsyncService {

    private final JavaMailSender mailSender;

    @Async("mailTaskExecutor")
    public void sendTicketCreatedEmail(String toEmail, String recipientName, String ticketCode, String title,
            String priority, LocalDateTime dueDate, String trackingUrl) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }

        String dueStr = dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
                : "Đang cập nhật";
        String subject = "[QAUTE Portal] Xác nhận tiếp nhận yêu cầu #" + ticketCode;

        String htmlContent = String.format(
                """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;">
                            <div style="background-color: #1e3a8a; padding: 20px; text-align: center; color: white;">
                                <h2 style="margin: 0;">QAUTE Portal - Cổng Tư Vấn Học Vụ</h2>
                            </div>
                            <div style="padding: 24px; color: #374151; line-height: 1.6;">
                                <p>Xin chào <strong>%s</strong>,</p>
                                <p>Hệ thống đã tiếp nhận yêu cầu tư vấn của bạn với thông tin chi tiết như sau:</p>
                                <table style="width: 100%%; border-collapse: collapse; margin: 16px 0;">
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #f3f4f6; color: #6b7280;">Mã Ticket:</td><td style="padding: 8px; border-bottom: 1px solid #f3f4f6; font-weight: bold; color: #1e3a8a;">%s</td></tr>
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #f3f4f6; color: #6b7280;">Tiêu đề:</td><td style="padding: 8px; border-bottom: 1px solid #f3f4f6;">%s</td></tr>
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #f3f4f6; color: #6b7280;">Mức độ ưu tiên:</td><td style="padding: 8px; border-bottom: 1px solid #f3f4f6;">%s</td></tr>
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #f3f4f6; color: #6b7280;">Hạn chót xử lý (SLA):</td><td style="padding: 8px; border-bottom: 1px solid #f3f4f6; font-weight: bold; color: #dc2626;">%s</td></tr>
                                </table>
                                <p>Bạn có thể theo dõi tiến độ xử lý và trao đổi với cán bộ phụ trách bằng cách nhấn vào nút bên dưới:</p>
                                <div style="text-align: center; margin: 24px 0;">
                                    <a href="%s" style="background-color: #1e3a8a; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Tra Cứu Tiến Độ Ticket</a>
                                </div>
                                <p style="font-size: 13px; color: #9ca3af;">Trân trọng,<br>Ban Tư Vấn Trường Đại học Sư phạm Kỹ thuật TP.HCM</p>
                            </div>
                        </div>
                        """,
                recipientName != null ? recipientName : "Bạn", ticketCode, title, priority, dueStr,
                trackingUrl != null ? trackingUrl : "#");

        sendHtmlMail(toEmail, subject, htmlContent);
    }

    @Async("mailTaskExecutor")
    public void sendTicketClaimedEmail(String toEmail, String recipientName, String ticketCode, String staffName,
            String departmentName, String trackingUrl) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }

        String subject = "[QAUTE Portal] Cán bộ đã tiếp nhận xử lý yêu cầu #" + ticketCode;
        String htmlContent = String.format(
                """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;">
                            <div style="background-color: #1e3a8a; padding: 20px; text-align: center; color: white;">
                                <h2 style="margin: 0;">QAUTE Portal - Thông Báo Xử Lý</h2>
                            </div>
                            <div style="padding: 24px; color: #374151; line-height: 1.6;">
                                <p>Xin chào <strong>%s</strong>,</p>
                                <p>Yêu cầu <strong>#%s</strong> của bạn đã được tiếp nhận xử lý bởi:</p>
                                <p style="background-color: #f8f9fa; padding: 12px; border-left: 4px solid #1e3a8a; border-radius: 4px;">
                                    <strong>Cán bộ phụ trách:</strong> %s<br>
                                    <strong>Đơn vị:</strong> %s
                                </p>
                                <div style="text-align: center; margin: 24px 0;">
                                    <a href="%s" style="background-color: #1e3a8a; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Xem Chi Tiết</a>
                                </div>
                            </div>
                        </div>
                        """,
                recipientName != null ? recipientName : "Bạn", ticketCode, staffName, departmentName,
                trackingUrl != null ? trackingUrl : "#");

        sendHtmlMail(toEmail, subject, htmlContent);
    }

    @Async("mailTaskExecutor")
    public void sendTicketResolvedEmail(String toEmail, String recipientName, String ticketCode, String replyContent,
            String trackingUrl) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }

        String subject = "[QAUTE Portal] Đã có kết quả giải đáp cho yêu cầu #" + ticketCode;
        String htmlContent = String.format(
                """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;">
                            <div style="background-color: #059669; padding: 20px; text-align: center; color: white;">
                                <h2 style="margin: 0;">QAUTE Portal - Kết Quả Giải Đáp</h2>
                            </div>
                            <div style="padding: 24px; color: #374151; line-height: 1.6;">
                                <p>Xin chào <strong>%s</strong>,</p>
                                <p>Yêu cầu <strong>#%s</strong> của bạn đã được giải đáp với nội dung như sau:</p>
                                <div style="background-color: #f0fdf4; border: 1px solid #bbf7d0; padding: 16px; border-radius: 6px; color: #166534; margin: 16px 0;">
                                    %s
                                </div>
                                <p>Vui lòng truy cập hệ thống để xác nhận đóng yêu cầu và đánh giá chất lượng phục vụ (1 - 5 sao):</p>
                                <div style="text-align: center; margin: 24px 0;">
                                    <a href="%s" style="background-color: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Đánh Giá & Đóng Ticket</a>
                                </div>
                            </div>
                        </div>
                        """,
                recipientName != null ? recipientName : "Bạn", ticketCode,
                replyContent != null ? replyContent.replace("\n", "<br>") : "Đã xử lý xong",
                trackingUrl != null ? trackingUrl : "#");

        sendHtmlMail(toEmail, subject, htmlContent);
    }

    @Async("mailTaskExecutor")
    public void sendSlaOverdueAlertEmail(String toEmail, String recipientName, String ticketCode, String title,
            LocalDateTime dueDate, String priority, String departmentName) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }

        String dueStr = dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
                : "Không xác định";
        String subject = "[CẢNH BÁO SLA] Yêu cầu #" + ticketCode + " đã quá hạn xử lý!";

        String htmlContent = String.format(
                """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #fecaca; border-radius: 8px; overflow: hidden;">
                            <div style="background-color: #dc2626; padding: 20px; text-align: center; color: white;">
                                <h2 style="margin: 0;">QAUTE Portal - Cảnh Báo Vi Phạm SLA</h2>
                            </div>
                            <div style="padding: 24px; color: #374151; line-height: 1.6;">
                                <p>Kính gửi <strong>%s</strong> (%s),</p>
                                <p>Hệ thống ghi nhận yêu cầu tư vấn sau đây đã <strong style="color: #dc2626;">QUÁ HẠN XỬ LÝ (OVERDUE)</strong> theo cam kết SLA của Nhà trường:</p>
                                <table style="width: 100%%; border-collapse: collapse; margin: 16px 0; background-color: #fef2f2; border: 1px solid #fee2e2; border-radius: 6px;">
                                    <tr><td style="padding: 10px; border-bottom: 1px solid #fee2e2; color: #6b7280;">Mã Ticket:</td><td style="padding: 10px; border-bottom: 1px solid #fee2e2; font-weight: bold; color: #dc2626;">#%s</td></tr>
                                    <tr><td style="padding: 10px; border-bottom: 1px solid #fee2e2; color: #6b7280;">Tiêu đề:</td><td style="padding: 10px; border-bottom: 1px solid #fee2e2; font-weight: bold;">%s</td></tr>
                                    <tr><td style="padding: 10px; border-bottom: 1px solid #fee2e2; color: #6b7280;">Mức ưu tiên:</td><td style="padding: 10px; border-bottom: 1px solid #fee2e2;">%s</td></tr>
                                    <tr><td style="padding: 10px; border-bottom: 1px solid #fee2e2; color: #6b7280;">Hạn chót cam kết:</td><td style="padding: 10px; border-bottom: 1px solid #fee2e2; font-weight: bold; color: #dc2626;">%s</td></tr>
                                </table>
                                <p>Đề nghị Thầy/Cô và Đơn vị khẩn trương tiếp nhận giải đáp hoặc phân công cán bộ xử lý dứt điểm nhằm đảm bảo quyền lợi cho sinh viên.</p>
                                <p style="font-size: 13px; color: #9ca3af; margin-top: 24px;">Thông báo tự động từ Hệ thống Giám sát SLA - QAUTE Portal HCMUTE.</p>
                            </div>
                        </div>
                        """,
                recipientName != null ? recipientName : "Cán bộ phụ trách",
                departmentName != null ? departmentName : "Đơn vị", ticketCode, title, priority, dueStr);

        sendHtmlMail(toEmail, subject, htmlContent);
    }

    @Async("mailTaskExecutor")
    public void sendOtpEmail(String toEmail, String recipientName, String otpCode, String purpose) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }

        String subject = "[QAUTE Portal] Mã xác thực OTP: " + otpCode;
        String actionText = (purpose != null && !purpose.isBlank()) ? purpose : "xác thực yêu cầu tài khoản";

        String htmlContent = String.format(
                """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
                            <div style="background: linear-gradient(135deg, #1e3a8a 0%%, #0f172a 100%%); padding: 24px; text-align: center; color: white;">
                                <h2 style="margin: 0; font-size: 20px;">QAUTE Portal - Cổng Tư Vấn HCMUTE</h2>
                                <p style="margin: 6px 0 0; font-size: 13px; opacity: 0.85;">Mã xác thực bảo mật tài khoản (OTP)</p>
                            </div>
                            <div style="padding: 28px 24px; color: #374151; line-height: 1.6;">
                                <p>Xin chào <strong>%s</strong>,</p>
                                <p>Hệ thống nhận được yêu cầu <strong>%s</strong> cho tài khoản gắn với email này.</p>
                                
                                <div style="text-align: center; margin: 28px 0;">
                                    <div style="display: inline-block; background-color: #f8fafc; border: 2px dashed #1e3a8a; border-radius: 10px; padding: 16px 36px;">
                                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #1e3a8a; font-family: 'Courier New', monospace;">%s</span>
                                    </div>
                                    <p style="font-size: 12px; color: #6b7280; margin-top: 8px;">Mã OTP có hiệu lực trong vòng <strong>5 phút</strong>. Tuyệt đối không chia sẻ mã này cho bất kỳ ai.</p>
                                </div>

                                <p style="font-size: 13px; color: #4b5563;">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này hoặc liên hệ Quản trị viên để bảo vệ tài khoản.</p>
                                <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 20px 0;">
                                <p style="font-size: 12px; color: #9ca3af; text-align: center;">Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE)<br>Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP.HCM</p>
                            </div>
                        </div>
                        """,
                recipientName != null ? recipientName : "Bạn",
                actionText,
                otpCode);

        sendHtmlMail(toEmail, subject, htmlContent);
    }

    private void sendHtmlMail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Gửi Async Email thành công tới: {} | Tiêu đề: {}", toEmail, subject);
        } catch (Exception e) {
            log.warn(
                    "Không thể gửi email tới {}: {} (Nếu chạy môi trường Dev, hãy cấu hình mail credentials trong application.yml)",
                    toEmail, e.getMessage());
        }
    }
}

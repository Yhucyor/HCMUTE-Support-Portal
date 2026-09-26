# BẢNG PHÂN BỔ TRÁCH NHIỆM & KHÓA HỢP ĐỒNG CODE: DEV A

* **Kỹ sư đảm nhiệm:** Dev A (Backend Core & AI/SLA Specialist)
* **Phạm vi trách nhiệm:** Module Auth (Session Cookie & RBAC), Module Ticket & SLA Engine (2 luồng tạo Ticket + Async Mail Guest/Student), Phân quyền Khoa/Phòng, Module AI RAG Chatbot.
* **Tài liệu tham chiếu:** [docs/brief.md](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/docs/brief.md) và [docs/team/engineering-rules.md](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/docs/team/engineering-rules.md)

---

## 1. Quyền Sở Hữu Tuyệt Đối (Ownership Package & Files)

> [!IMPORTANT]
> **Quy tắc bất khả xâm phạm:** Dev A nắm toàn quyền phát triển và bảo trì các package sau. Dev B tuyệt đối không sửa đổi hoặc commit vào các package này mà không có sự đồng thuận.

```text
com.school.counseling
  ├── config/
  │     ├── SecurityConfig.java         (Spring Security Session & CSRF)
  │     ├── AsyncConfig.java            (Async Mail Executor)
  │     └── AiConfig.java               (Cấu hình AI RAG & Embeddings)
  ├── module/
  │     ├── auth/                       (Toàn bộ Controller, Service, DTO, Repository)
  │     ├── ticket/                     (Toàn bộ Ticket, SLA Engine, Messages, Guest Access)
  │     ├── ai/                         (Toàn bộ RAG Service, Semantic Search, AiChatController)
  │     └── notification/               (EmailAsyncService, NotificationLogs)
  └── templates/
        ├── auth/                       (login.html, register.html, verify-otp.html)
        ├── ticket/                     (list.html, detail.html, create.html, guest-track.html)
        └── ai/                         (chat-widget.html, rag-dialog.html)
```

---

## 2. Locked Interface Contract (Hợp Đồng Code Khóa Cứng Cho Dev A)

Dev A bắt buộc phải triển khai hoặc cung cấp các Interface chuẩn sau để Dev B có thể gọi an toàn:

### 2.1. `IUserService` (Cung cấp thông tin User & Department)
```java
package com.school.counseling.module.auth.service;

import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.Department;
import java.util.Optional;

public interface IUserService {
    User getCurrentAuthenticatedUser();
    Optional<User> findById(Long userId);
    Optional<User> findByUsername(String username);
    Department getUserDepartment(Long userId);
    boolean isStaffOfDepartment(Long userId, Long departmentId);
}
```

### 2.2. `IAiRagService` (Dịch vụ Trả lời Tri thức AI & Gợi ý Fallback Ticket)
```java
package com.school.counseling.module.ai.service;

import java.util.List;

public interface IAiRagService {
    AiResponseDto generateAnswer(String userQuestion, Long departmentId);
    boolean shouldFallbackToTicket(AiResponseDto aiResponse);
    void indexDocument(Long documentId, String fileUrl, String sourceContent);

    record AiResponseDto(
        String answer,
        double confidenceScore,
        List<String> sourceCitations,
        boolean canCreateTicketPrompt,
        Long suggestedDepartmentId
    ) {}
}
```

### 2.3. `INotificationService` (Gửi Email & Thông Báo Bất Đồng Bộ)
```java
package com.school.counseling.module.notification.service;

public interface INotificationService {
    void sendTicketStatusEmailAsync(String toEmail, String recipientName, String ticketCode, String newStatus, String link);
    void sendPostApprovalNotificationAsync(Long authorId, Long postId, boolean isApproved, String reason);
    void sendPostReportAlertToStaffAsync(Long departmentId, Long postId, String reason);
}
```

---

## 3. Danh Mục Deliverables Của Dev A Theo Sprint

* **Sprint 1:**
  * Cấu hình Spring Security với Session Cookie `JSESSIONID` (`HttpOnly`, `SameSite=Lax`, CSRF).
  * Luồng đăng ký tài khoản sinh viên với OTP Email (5 phút hiệu lực).
  * Seed Data người dùng mẫu (Admin, Staff các Khoa/Phòng, Sinh viên).
* **Sprint 2:**
  * SLA Engine tính toán `due_date` tự động (`URGENT: 24h`, `MEDIUM: 72h`, `LOW: 7d`).
  * Giao diện và API tạo Ticket (2 luồng: Direct Form + Guest Token + Chuyển đổi từ Chat).
  * Màn hình quản lý Ticket cho Staff phân quyền nghiêm ngặt theo Khoa/Phòng (`BRULE-TICKET-004`).
  * Tích hợp `JavaMailSender` bất đồng bộ (`@Async`).
* **Sprint 4:**
  * Triển khai `IAiRagService` kết nối bộ dữ liệu 2.672 Q&A và tài liệu quy chế.
  * Tích hợp Chatbot Widget với cơ chế tự động gợi ý chuyển thành Ticket khi độ tin cậy thấp.

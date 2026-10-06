# 📋 PHÂN CÔNG NHIỆM VỤ: DEV A (BACKEND LEAD & DATABASE ARCHITECT)

* **Họ và tên:** Dev A
* **Vị trí đảm nhận:** Backend Lead & Database Architect
* **Phạm vi trách nhiệm:** Toàn bộ Cơ sở dữ liệu (Database 17 bảng) & Hệ thống Backend Spring Boot 3
* **Tài liệu quy chuẩn tham chiếu:** [engineering-rules.md](file:///d:/HK5/LapTrinhWeb/doancuoiki/HCMUTE-Support-Portal/docs/team/engineering-rules.md)

---

## 🎯 1. TỔNG QUAN PHẠM VI CÔNG VIỆC

Dev A chịu trách nhiệm độc lập và toàn diện về tầng dữ liệu, mô hình hóa nghiệp vụ, các dịch vụ xử lý logic (Service), bảo mật (Spring Security), xử lý bất đồng bộ (Async/Cron Job/WebSocket), thuật toán AI RAG và hệ thống REST/Web Controllers.

---

## 🗄️ 2. CƠ SỞ DỮ LIỆU & KIẾN TRÚC DỮ LIỆU (DATABASE - 17 BẢNG)

Dev A chịu trách nhiệm thiết kế, tối ưu chỉ mục (Indexing), chuẩn hóa 3NF và triển khai DDL `schema.sql` cho toàn bộ 17 bảng trong hệ thống:

### 2.1. Nhóm Bảng Cốt Lõi (12 Bảng)
1. `roles`: Quản lý 3 vai trò chính (`ROLE_STUDENT`, `ROLE_STAFF`, `ROLE_ADMIN`).
2. `departments`: 5 Khoa/Phòng ban đầu mối (Đoàn Hội, Tuyển sinh, Đào tạo, Khoa CNTT, Khoa Ngoại ngữ).
3. `users`: Tài khoản sinh viên, giảng viên, cán bộ quản trị (RBAC, Department Mapping).
4. `otp_tokens`: Quản lý mã OTP 6 số xác thực đăng ký và đặt lại mật khẩu.
5. `tickets`: Quản lý phiếu tư vấn học vụ (Mã ticket, Tiêu đề, Khoa tiếp nhận, Trạng thái SLA).
6. `ticket_messages`: Nhật ký trao đổi, phản hồi giữa sinh viên và cán bộ tư vấn.
7. `ticket_histories`: Audit log ghi lại mọi lịch sử chuyển trạng thái, đổi cán bộ xử lý, thay đổi độ ưu tiên.
8. `posts`: Bài viết diễn đàn sinh viên, bài thông báo khoa/phòng ban.
9. `post_attachments`: Tệp đính kèm hình ảnh/tài liệu trong bài đăng.
10. `post_comments`: Bình luận thảo luận dưới bài viết (hỗ trợ phân cấp cha/con).
11. `post_reactions`: Lượt thích (Like) bài viết.
12. `post_reports`: Báo cáo vi phạm nội dung từ sinh viên.

### 2.2. Nhóm Bảng Mở Rộng & Nâng Cao (5 Bảng)
13. `notification_logs`: Lưu vết lịch sử thông báo hệ thống và chuông realtime.
14. `ai_knowledge_documents`: Tài liệu tri thức gốc phục vụ băm nhỏ (chunking) cho AI RAG.
15. `knowledge_chunks`: Các đoạn văn bản tri thức đã băm nhỏ kèm Vector Embedding để tìm kiếm ngữ nghĩa.
16. `faqs`: Bộ câu hỏi thường gặp (FAQ) được cán bộ khoa kiểm duyệt để đối khớp tự động.
17. `internal_staff_admin_messages`: Kênh trao đổi nội bộ bảo mật giữa Cán bộ Khoa và Quản trị viên.

---

## ⚙️ 3. BACKEND MODULES & NGHIỆP VỤ XỬ LÝ (SPRING BOOT 3)

Dev A phát triển toàn bộ 6 Module nghiệp vụ Backend theo chuẩn Spring Boot 3.3.x / Java 17:

### 3.1. Module 1: Xác Thực, Phân Quyền & Bảo Mật (Auth & RBAC)
- **Spring Security 6 Configuration:** Cấu hình Form Login session song song với JWT Bearer filter cho REST API.
- **Dịch vụ User & Profile:** Đăng ký, đăng nhập, quên mật khẩu qua OTP, đổi mật khẩu, upload ảnh đại diện.
- **Department Isolation Security SpEL:** Triển khai bean `@deptSecurity.hasAccess(authentication, departmentId)` nhằm đảm bảo cán bộ khoa này không bao giờ xem được dữ liệu độc quyền của khoa khác.

### 3.2. Module 2: Quản Lý Ticket Tư Vấn & SLA Engine
- **Vòng đời Ticket:** `OPEN` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `RESOLVED` $\rightarrow$ `CLOSED` $\rightarrow$ `REJECTED`.
- **Cơ chế Claim & Ủy quyền:** Cán bộ tiếp nhận vé, chuyển vé liên khoa, ủy quyền chuyên viên.
- **Hệ thống cảnh báo SLA:** Tự động tính toán hạn phản hồi (Response SLA: 24h) và hạn xử lý dứt điểm (Resolve SLA: 72h).
- **Scheduled Auto-Close Cron:** Tự động đóng các ticket đã `RESOLVED` sau 48h nếu sinh viên không có khiếu nại.

### 3.3. Module 3: Diễn Đàn Sinh Viên, Nhóm Khoa & Kiểm Duyệt Tự Động
- **Bảng tin & Nhóm:** Đăng bài, tương tác Like/Bình luận, đính kèm đa phương tiện.
- **Bộ lọc từ khóa cấm (Banned Keyword Filter):** Tự động phát hiện và chuyển bài viết sang trạng thái `PENDING_REVIEW` hoặc ẩn nếu phát hiện nội dung xúc phạm, spam.
- **Quy trình xử lý Report:** Cho phép Admin/Staff phê duyệt hoặc bác bỏ báo cáo bài viết.

### 3.4. Module 4: Thông Báo Realtime & Gửi Email Bất Đồng Bộ
- **WebSocket STOMP Channel:** Bắn thông báo chuông trực tiếp khi có phản hồi ticket mới hoặc bài viết được duyệt.
- **Async Mail Service:** Tích hợp `@Async` với `ThreadPoolTaskExecutor` chuyên dụng, gửi email thông báo xác thực OTP, phản hồi ticket mà không block luồng xử lý HTTP chính.

### 3.5. Module 5: Trợ Lý AI RAG & Đối Khớp FAQ
- **RAG Knowledge Chunking:** Chia nhỏ tài liệu học vụ, quy chế đào tạo thành các chunk 500-1000 ký tự.
- **Cosine Similarity Matcher:** Thuật toán tính độ tương đồng ngữ nghĩa giữa câu hỏi người dùng và vector tri thức.
- **Fallback-to-Ticket Algorithm:** Nếu độ tự tin của AI $< 0.70$, tự động gợi ý và tạo phiếu Ticket chuyển thẳng đến Cán bộ Khoa tương ứng.

### 3.6. Module 6: Báo Cáo Thống Kê & Kênh Nội Bộ Cán Bộ (Admin Desk)
- **KPI & SLA Analytics:** Tổng hợp tỷ lệ xử lý đúng hạn, số lượng ticket quá hạn theo từng Khoa/Phòng ban.
- **Xuất báo cáo dữ liệu:** Xuất file thống kê dữ liệu học vụ CSV / Excel.
- **Kênh tin nhắn nội bộ:** Trao đổi bảo mật giữa Admin và Staff.

---

## 📦 4. SẢN PHẨM BÀN GIAO CỦA DEV A
1. File DDL hoàn chỉnh: `docs/database/schema.sql` (17 bảng + Sample Data).
2. Toàn bộ mã nguồn Java: Entities, Repositories, DTOs, Services, Configs, Controllers.
3. Đảm bảo toàn bộ mã nguồn compile thành công với Maven (`mvn clean compile` -> `BUILD SUCCESS`).

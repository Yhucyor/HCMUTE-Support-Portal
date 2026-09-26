# DANH SÁCH NHIỆM VỤ & TIÊU CHÍ NGHIỆM THU: DEV A

* **Kỹ sư đảm nhiệm:** Dev A (Backend Core, SLA Engine, Email & AI RAG)
* **Quy chuẩn thực thi:** [docs/brief.md](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/docs/brief.md), [docs/team/engineering-rules.md](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/docs/team/engineering-rules.md)

---

## 🟢 SPRINT 1: XÁC THỰC, BẢO MẬT & QUẢN TRỊ NGƯỜI DÙNG

- [ ] **`[DEV-A-SP1-01]` Thiết lập Cấu hình Bảo mật Spring Security Session Cookie & CSRF**
  * **File:** `config/SecurityConfig.java`, `module/auth/service/UserDetailsServiceImpl.java`
  * **Ràng buộc nghiệp vụ:** `BRULE-AUTH-002`, `BRULE-AUTH-003`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* người dùng chưa đăng nhập khi truy cập `/tickets/create`.
    * *When* gửi request tới endpoint bảo vệ.
    * *Then* hệ thống chuyển hướng đến `/auth/login`, và sau khi đăng nhập thành công cấp Session Cookie `JSESSIONID` có cờ `HttpOnly`, `SameSite=Lax`.
  * **Manual Test Procedure:**
    * *Bước 1:* Dùng trình duyệt ẩn danh vào `http://localhost:8080/tickets/my-tickets`.
    * *Kỳ vọng UI:* Chuyển hướng về `/auth/login`. Kiểm tra F12 -> Application -> Cookies thấy `JSESSIONID` có thuộc tính `HttpOnly`.
    * *Kỳ vọng DB:* Bảng `users` lưu `password_hash` bắt đầu bằng `$2a$10$...` (BCrypt).

- [ ] **`[DEV-A-SP1-02]` Đăng Ký Tài Khoản Sinh Viên & Xác Thực OTP Qua Email**
  * **File:** `module/auth/controller/AuthController.java`, `module/auth/service/AuthService.java`, `templates/auth/register.html`, `templates/auth/verify-otp.html`
  * **Ràng buộc nghiệp vụ:** `BRULE-AUTH-001`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* sinh viên điền thông tin đăng ký với email trường hợp lệ.
    * *When* nhấn "Đăng ký", hệ thống sinh mã OTP 6 chữ số gửi qua email và lưu vào `otp_tokens` với hạn 5 phút.
    * *Then* sinh viên nhập đúng OTP thì tài khoản đổi từ `PENDING_ACTIVATION` sang `ACTIVE`.
  * **Manual Test Procedure:**
    * *Input:* Username: `sv2026_01`, Email: `test_sv@hcmute.edu.vn`, Password: `Password123@`.
    * *Kỳ vọng UI:* Hiển thị màn hình nhập OTP 6 ô số. Nhập đúng -> Thông báo thành công và chuyển sang trang Đăng nhập.
    * *Kỳ vọng DB:* Bảng `otp_tokens` ghi nhận `is_used = true`; bảng `users` cập nhật `status = 'ACTIVE'`.

---

## 🟢 SPRINT 2: TICKET WORKFLOW, ĐỘNG CƠ TÍNH SLA & EMAIL BẤT ĐỒNG BỘ

- [ ] **`[DEV-A-SP2-01]` Xây Dựng Động Cơ Tính Deadline SLA (SlaCalculatorService)**
  * **File:** `module/ticket/service/SlaCalculatorService.java`
  * **Ràng buộc nghiệp vụ:** `BRULE-TICKET-002`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* một yêu cầu tư vấn mới được tạo với `priority` cụ thể.
    * *When* Service tính toán `due_date`.
    * *Then* nếu `URGENT` -> `due_date = now + 24h`; `MEDIUM` -> `due_date = now + 72h`; `LOW` -> `due_date = now + 7 ngày`.
  * **Manual Test Procedure:**
    * *Input:* Gửi tạo Ticket chọn mức ưu tiên "Khẩn cấp (URGENT)".
    * *Kỳ vọng DB:* Bảng `tickets` có cột `due_date` đúng bằng `created_at` cộng 24 giờ (dung sai $\le 2$ giây).

- [ ] **`[DEV-A-SP2-02]` Tiếp Nhận Ticket Cho Guest & Sinh Viên (2 Luồng Tạo)**
  * **File:** `module/ticket/controller/TicketController.java`, `module/ticket/service/TicketService.java`, `templates/ticket/create.html`, `templates/ticket/guest-track.html`
  * **Ràng buộc nghiệp vụ:** `BRULE-TICKET-001`, `BRULE-TICKET-006`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* một khách vãng lai (Guest) chưa đăng nhập điền form tư vấn tuyển sinh kèm Email.
    * *When* submit Form tạo Ticket.
    * *Then* hệ thống tạo mã `guest_access_token` (UUID), lưu `tickets` với `creator_id = NULL` và gửi email chứa đường link tra cứu bí mật `/tickets/guest-track?token=...`.
  * **Manual Test Procedure:**
    * *Input:* Guest Name: "Nguyễn Văn Thí Sinh", Email: "thisinh@gmail.com", Chọn Phòng Tuyển sinh, Tiêu đề: "Hỏi điểm chuẩn 2026".
    * *Kỳ vọng UI:* Hiển thị màn hình thành công kèm mã Ticket `TK-20260905-XXXX`. Truy cập bằng link token xem được tiến độ.
    * *Kỳ vọng DB:* Bảng `tickets` có `guest_email = 'thisinh@gmail.com'`, `guest_access_token IS NOT NULL`.

- [ ] **`[DEV-A-SP2-03]` Phân Luồng Xử Lý Ticket Cho Cán Bộ Khoa/Phòng (Claiming & No Cross-Access)**
  * **File:** `module/ticket/controller/StaffTicketController.java`, `templates/ticket/staff-list.html`, `templates/ticket/detail.html`
  * **Ràng buộc nghiệp vụ:** `BRULE-AUTH-005`, `BRULE-TICKET-003`, `BRULE-TICKET-004`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* Cán bộ Khoa CNTT đăng nhập vào hệ thống.
    * *When* truy cập danh sách Ticket.
    * *Then* Cán bộ chỉ nhìn thấy Ticket thuộc `department_id = 4 (Khoa CNTT)`. Khi bấm "Tiếp nhận", ticket đổi sang `IN_PROGRESS` và gán `assigned_to = staff_id`.
  * **Manual Test Procedure:**
    * *Input:* Đăng nhập tài khoản Staff Khoa CNTT (`staff_cntt`). Cố tình gõ URL `/staff/tickets/detail/5` (vốn là Ticket của Phòng Tuyển sinh).
    * *Kỳ vọng UI:* Trả về trang lỗi `403.html` (Không có quyền can thiệp ngoài đơn vị).
    * *Kỳ vọng DB:* Khi bấm Tiếp nhận trên Ticket Khoa mình, `tickets.assigned_to` cập nhật ID của `staff_cntt`.

- [ ] **`[DEV-A-SP2-04]` Dịch Vụ Gửi Email Bất Đồng Bộ (Async Email Service)**
  * **File:** `module/notification/service/EmailAsyncService.java`, `config/AsyncConfig.java`
  * **Ràng buộc nghiệp vụ:** `BRULE-TICKET-005`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* Cán bộ gửi câu trả lời và chuyển trạng thái Ticket sang `RESOLVED`.
    * *When* Ticket cập nhật thành công.
    * *Then* hệ thống kích hoạt luồng `@Async` gửi email thông báo kết quả giải đáp đến sinh viên/guest, không gây trễ giao diện web.
  * **Manual Test Procedure:**
    * *Kỳ vọng Console/Log:* Log gửi mail xuất hiện trên thread pool `async-mail-task-X`. Bảng `notification_logs` ghi nhận 1 dòng trạng thái `SUCCESS`.

---

## 🟢 SPRINT 4: TÍCH HỢP TRỢ LÝ TRI THỨC HỌC VỤ AI RAG

- [ ] **`[DEV-A-SP4-01]` Nạp Bộ Tri Thức 2.672 Q&A & Tài Liệu Quy Chế (Knowledge Ingestion)**
  * **File:** `module/ai/service/VectorEmbeddingService.java`, `module/ai/entity/KnowledgeDocument.java`
  * **Ràng buộc nghiệp vụ:** `BRULE-RAG-001`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* file dữ liệu `form_demo/faq_dataset.json` chứa 2.672 Q&A thực tế.
    * *When* chạy seed data AI.
    * *Then* dữ liệu được phân đoạn (chunking), đánh chỉ mục và lưu trữ sẵn sàng cho tìm kiếm ngữ nghĩa.
  * **Manual Test Procedure:**
    * *Kỳ vọng DB:* Bảng `ai_knowledge_documents` ghi nhận các danh mục tài liệu với `indexed_status = 'READY'`.

- [ ] **`[DEV-A-SP4-02]` Chatbot Trả Lời Ngữ Nghĩa & Fallback Tạo Ticket Tự Động**
  * **File:** `module/ai/controller/AiChatController.java`, `module/ai/service/RagService.java`, `templates/ai/chat-widget.html`
  * **Ràng buộc nghiệp vụ:** `BRULE-RAG-001`, `BRULE-TICKET-001`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* sinh viên mở khung Chatbot hỏi "Quy định nộp chứng chỉ tiếng Anh đầu vào?".
    * *When* AI tìm kiếm và trả về câu trả lời với trích dẫn phòng ban.
    * *Then* nếu sinh viên hỏi câu hỏi quá khó hoặc không có trong tài liệu, Chatbot hiển thị nút "Chuyển thành Ticket gửi Phòng Đào tạo" và tự động điền nội dung câu hỏi vào Form Ticket.
  * **Manual Test Procedure:**
    * *Input trên Chat:* "Em bị trùng lịch thi cuối kỳ môn Toán 2 thì làm sao?".
    * *Kỳ vọng UI:* AI trả lời hướng dẫn làm đơn hoãn thi tại A1-201. Nhấn "Gửi Ticket" -> Mở modal tạo Ticket đã điền sẵn tiêu đề và nội dung.

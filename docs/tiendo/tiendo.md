# BÁO CÁO ĐÁNH GIÁ TIẾN ĐỘ DỰ ÁN QAUTE PORTAL
**Thời điểm kiểm tra:** 06/09/2026  
**Cơ sở đối chiếu:** Tài liệu phân công [docs/team/dev-a-tasks.md](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/docs/team/dev-a-tasks.md), [docs/team/dev-b-tasks.md](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/docs/team/dev-b-tasks.md) & Mã nguồn thực tế tại `src/`.

---

## 📊 1. BẢNG TỔNG QUAN TIẾN ĐỘ THỰC TẾ

| Nhân sự | Phạm vi trách nhiệm | Sprint 1 | Sprint 2 / 3 | Sprint 4 | Tỷ lệ hoàn thành | Đánh giá tổng thể |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Dev A** | Backend Core, Security RBAC, Ticket SLA, Email Async, AI FAQ/Chatbot | **85%** | **95%** | **80%** | **~85 - 90%** | 🟢 **Đúng tiến độ, chất lượng cao** |
| **Dev B** | UI Layout, Storage Service, Feed Bảng tin & Diễn đàn, Duyệt bài, Webhook Video | **60%** | **0%** | - | **~25 - 30%** | 🟡 **Chậm tiến độ Sprint 3** |

---

## 👨‍💻 2. TIẾN ĐỘ CHI TIẾT CỦA DEV A

### 🟢 Các phần việc ĐÃ HOÀN THÀNH:

#### 1. Sprint 1: Xác thực, Phân quyền & Quản lý Người dùng
- ✅ **Cấu hình Spring Security ([SecurityConfig.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/config/SecurityConfig.java)):**
  - Cấu hình chuẩn Form Login, Session Cookie (`JSESSIONID` cờ `HttpOnly`, `SameSite=Lax`), CSRF protection, mã hóa mật khẩu `BCryptPasswordEncoder`.
  - Phân quyền theo Role: `ADMIN`, `STAFF`, `STUDENT`.
- ✅ **Dịch vụ User Details ([UserDetailsServiceImpl.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/auth/service/UserDetailsServiceImpl.java), [UserPrincipal.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/auth/dto/UserPrincipal.java)):**
  - Load đầy đủ thông tin User, Quyền hạn và Đơn vị Khoa/Phòng từ Database.
- ✅ **Core Entities ([User.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/auth/entity/User.java), [Role.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/auth/entity/Role.java), [Department.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/auth/entity/Department.java)):**
  - Hoàn thiện mapping JPA quan hệ giữa User, Role, Department.
- ✅ **Giao diện & Controller Auth ([AuthWebController.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/auth/controller/AuthWebController.java), [login.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/auth/login.html), [register.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/auth/register.html)):**
  - Xử lý đăng nhập, đăng ký tài khoản sinh viên.

#### 2. Sprint 2: Ticket Workflow, SLA Engine & Email Bất đồng bộ
- ✅ **Động cơ tính SLA ([SlaCalculatorService.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/ticket/service/SlaCalculatorService.java)):**
  - Tự động tính toán `due_date` chính xác theo mức ưu tiên: `URGENT` (24h), `HIGH` (48h), `MEDIUM` (72h), `LOW` (7 ngày) tuân thủ `BRULE-TICKET-002`.
- ✅ **Luồng Tiếp nhận Ticket 2 Kênh ([TicketService.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/ticket/service/TicketService.java), [TicketWebController.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/ticket/controller/TicketWebController.java)):**
  - Kênh 1: Sinh viên đã đăng nhập tạo Ticket trực tiếp.
  - Kênh 2: Khách vãng lai (Guest) tạo Ticket, sinh mã `guestAccessToken` (UUID) và gửi link tra cứu bí mật `/tickets/guest-track`.
- ✅ **Phân quyền Ticket cho Staff & Chống Cross-Access ([StaffTicketWebController.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/ticket/controller/StaffTicketWebController.java), [DepartmentAccessEvaluator.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/auth/service/DepartmentAccessEvaluator.java)):**
  - Cán bộ chỉ xem và xử lý Ticket thuộc Khoa/Phòng của mình, ngăn chặn IDOR và truy cập chéo đơn vị (`BRULE-TICKET-004`).
- ✅ **Gửi Email Bất đồng bộ ([EmailAsyncService.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/notification/service/EmailAsyncService.java), [AsyncConfig.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/config/AsyncConfig.java)):**
  - Cấu hình `@Async("mailTaskExecutor")`, tự động gửi mail thông báo cho sinh viên/guest khi tạo ticket hoặc có phản hồi giải đáp mà không làm chậm giao diện.
- ✅ **Giao diện Ticket Hoàn chỉnh:**
  - [create.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/ticket/create.html), [detail.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/ticket/detail.html), [guest-track.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/ticket/guest-track.html), [my-tickets.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/ticket/my-tickets.html), [staff-dashboard.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/ticket/staff-dashboard.html).

#### 3. Sprint 4: Trợ lý Tri thức Học vụ AI & FAQ Semantic Search
- ✅ **Dịch vụ Tìm kiếm Ngữ nghĩa FAQ ([SmartFaqMatcherService.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/ai/service/SmartFaqMatcherService.java)):**
  - Nạp 2.672 Q&A thực tế, kết hợp thuật toán Jaccard + Levenshtein + Keyword Matching để tìm câu trả lời tức thì.
- ✅ **Giao diện Tra cứu & Chat ([FaqWebController.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/ai/controller/FaqWebController.java), [chat-widget.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/ai/chat-widget.html), [search.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/faq/search.html)):**
  - Widget chat góc màn hình và trang tra cứu câu hỏi thường gặp theo Khoa/Phòng.

---

### ⚠️ Phần việc Dev A CẦN BỔ SUNG / HOÀN THIỆN:
- ⏳ **Xác thực OTP Email 6 số khi Đăng ký (`[DEV-A-SP1-02]`):** Hiện tại Form Đăng ký đang tạo thẳng tài khoản `STUDENT` mà chưa gửi mã OTP kích hoạt tài khoản có hạn 5 phút vào email sinh viên (`verify-otp.html`).

---

## 👨‍💻 3. TIẾN ĐỘ CHI TIẾT CỦA DEV B

### 🟢 Các phần việc ĐÃ HOÀN THÀNH:

#### 1. Sprint 1: Khung Layout & Dịch vụ Lưu trữ
- ✅ **Layout Học viện Tối giản (Academic Minimalist):**
  - Hoàn thiện khung chuẩn Bootstrap 5 tại [main.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/layout/main.html), [navbar.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/layout/navbar.html), [footer.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/layout/footer.html) với tông màu Navy (`#1e3a8a`) & Xám sáng (`#f8f9fa`), tương thích Desktop và Mobile.
- ✅ **Trang lỗi tập trung ([templates/error/](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/error)):**
  - Đã có [403.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/error/403.html), [404.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/error/404.html), [500.html](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/templates/error/500.html).
- ✅ **Dịch vụ Lưu trữ Cục bộ ([LocalStorageServiceImpl.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/common/storage/LocalStorageServiceImpl.java)):**
  - Đã triển khai interface [IStorageService.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/common/storage/IStorageService.java) lưu trữ tệp vào thư mục `uploads/`.

---

### 🔴 Các phần việc Dev B CHƯA THỰC HIỆN (Chậm tiến độ Sprint 3):

1. ❌ **Lưu trữ Đám mây & Kiểm tra Magic Bytes (`[DEV-B-SP1-02]`):**
   - Chưa có `CloudinaryStorageServiceImpl.java` để hỗ trợ chế độ Dual Storage.
   - Chưa tích hợp kiểm tra Magic Bytes chống upload file thực thi giả mạo đuôi tài liệu.
2. ❌ **Module Bảng tin Chính thức (`[DEV-B-SP3-01]`):**
   - Chưa có package `com.school.counseling.module.feed`.
   - Chưa có Controller (`OfficialFeedController`), Service (`PostService`), Entity (`Post`, `PostAttachment`).
   - Chưa có giao diện `official-list.html`, `official-detail.html`, `create-official-post.html`.
3. ❌ **Module Diễn đàn Sinh viên & Luồng Phê duyệt Bài (`[DEV-B-SP3-02]`):**
   - Chưa có luồng đăng bài chờ duyệt (`PENDING_APPROVAL` -> `APPROVED`).
   - Chưa có `ForumController`, `ModerationController`, giao diện `forum.html`, `pending-posts.html`.
4. ❌ **Tương tác Like, Comment & Báo cáo Vi phạm (`[DEV-B-SP3-03]`):**
   - Chưa có Entity `Comment`, `PostReport`.
   - Chưa có giao diện `reports-list.html` để Cán bộ xử lý báo cáo vi phạm.
5. ❌ **Tiếp nhận Video từ Node.js Webhook (`[DEV-B-SP3-04]`):**
   - Chưa có package `com.school.counseling.module.integration`.
   - Chưa có `NodejsWebhookController` và `WebhookSecurityService` (xác thực HMAC-SHA256).

---

## 🎯 4. KẾ HOẠCH HÀNH ĐỘNG TIẾP THEO (NEXT ACTIONS)

1. **Ưu tiên 1 (Khẩn cấp cho Dev B):** 
   - Khởi tạo package `com.school.counseling.module.feed` và tạo các Entity cốt lõi (`Post`, `PostAttachment`, `Comment`, `PostReport`).
   - Xây dựng luồng đăng bài Bảng tin chính thức và Diễn đàn sinh viên.
2. **Ưu tiên 2 (Dev A):**
   - Bổ sung luồng gửi OTP Email 6 số khi đăng ký tài khoản sinh viên để hoàn tất 100% Sprint 1.
3. **Ưu tiên 3 (Dev B):**
   - Xây dựng `NodejsWebhookController` tiếp nhận video render tự động từ Node.js microservice.

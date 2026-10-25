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

---

## 🏛️ 5. ĐÁNH GIÁ HIỆN TRẠNG PHÂN HỆ ADMIN WORKSPACE & DỰ ĐỊNH CẢI TIẾN (CẬP NHẬT 25/10/2026)

### 5.1. Bảng Đánh Giá Hiện Trạng 4 Màn Hình Quản Trị Cốt Lõi

| Trang quản trị | Tuyến đường (Route) | File Template | Công nghệ UI hiện tại | Trạng thái kỹ thuật | Điểm nghẽn & Hạn chế cần cải tiến |
| :--- | :--- | :--- | :---: | :---: | :--- |
| **1. SLA Dashboard** | `GET /admin/dashboard` | [dashboard.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/admin/dashboard.html) | Bootstrap 5 + Chart.js | 🟢 Hoạt động tốt | - Biểu đồ tải tĩnh khi load trang, chưa tự cập nhật khi có ticket vi phạm SLA.<br>- Thiếu tính năng Xuất báo cáo SLA (Export Excel / PDF) gửi Ban Giám hiệu.<br>- Màu sắc biểu đồ còn cơ bản, chưa theo bảng màu thương hiệu HCMUTE. |
| **2. Quản Trị Người Dùng** | `GET /admin/users` | [users.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/admin/users.html) | Bootstrap 5 Table + AJAX | 🟢 Hoạt động tốt | - Thiếu Thao tác hàng loạt (Bulk Actions: Khóa, Mở khóa, Đổi Role nhiều user cùng lúc).<br>- Chưa có chức năng Import danh sách Cán bộ / Sinh viên từ file Excel (.xlsx).<br>- Modal Reset mật khẩu chưa có tùy chọn gửi thông báo mật khẩu tạm qua Email. |
| **3. Quản Lý Khoa / Phòng Ban** | `GET /admin/departments` | [departments.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/admin/departments.html) | Bootstrap 5 Table + Modal | 🟢 Hoạt động tốt | - Chưa hiển thị trực quan Trưởng đơn vị (Head of Dept) và danh sách chuyên viên trực ca.<br>- Cấu hình SLA đang áp dụng chung toàn trường, chưa cho phép tùy biến thời hạn SLA riêng cho từng phòng ban đặc thù.<br>- Thiếu chỉ số đánh giá tỷ lệ xử lý đúng hạn của từng đơn vị. |
| **4. Kênh Chỉ Đạo Nội Bộ** | `GET /admin/messages` | [internal-messages.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/admin/internal-messages.html) | Bootstrap 5 Chat Split Pane | 🟡 Cần nâng cấp | - Đang dùng cơ chế Polling / Refresh thủ công, chưa tận dụng WebSocket STOMP `/topic/internal-messages` để tin nhắn nảy tức thì.<br>- Chưa hỗ trợ gửi đính kèm văn bản chỉ đạo (PDF, Word, Ảnh minh chứng). |

---

### 5.2. Đánh Giá Trải Nghiệm Người Dùng (UI/UX) & Tính Đồng Bộ Hệ Thống
1. **Lệch pha ngôn ngữ thiết kế (Design System Disparity)**:
   - Trong khi toàn bộ Cổng sinh viên (Home, Tra cứu FAQ, Tạo Ticket, Hồ sơ cá nhân, Thời khóa biểu, Điểm số, Bảng tin) đã được nâng cấp lên chuẩn **Tailwind CSS + Google Material Symbols + Academic Horizon Design System** (tông màu `#003882`, bo góc 16-24px, đổ bóng mềm, Typography Outfit/Inter cao cấp).
   - Thì phân hệ Admin Workspace vẫn đang sử dụng **Bootstrap 5 cổ điển** kết hợp Bootstrap Icons (`bi-*`), tạo cảm giác giao diện quản trị bị tách biệt và chưa đồng bộ vẻ đẹp hiện đại của toàn hệ thống.
2. **Khoảng cách và Sub-Banner Điều Hành**:
   - Sub-banner [portal-header.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/layout/portal-header.html) hoạt động rất tốt, chia rõ tab Cán bộ và Admin. Cần giữ vững cấu trúc phân quyền này nhưng nâng cấp giao diện bảng và thẻ KPI sang phong cách hiện đại hơn.

---

### 5.3. Kế Hoạch & Dự Định Cải Tiến Toàn Diện (Roadmap Cải Tiến)

#### 🔹 Giai đoạn 1: Chuẩn hóa Giao diện & Trải nghiệm (Modern Admin UI Re-skin)
- [ ] **Đồng bộ hóa giao diện sang chuẩn Academic Horizon**:
  - Chuyển đổi bảng dữ liệu sang phong cách Flat-Modern với hiệu ứng hover mượt mà, phân trang hiện đại.
  - Tối ưu hóa các thẻ thống kê KPI SLA trên Dashboard: Hiển thị thanh tiến độ trực quan, màu cảnh báo thông minh (Xanh lá >95%, Vàng 85-95%, Đỏ <85%).
  - Tinh chỉnh typography dùng font Inter/Outfit đồng bộ với Cổng thông tin HCMUTE.

#### 🔹 Giai đoạn 2: Tính năng Nghiệp vụ Quản trị Chuyên sâu (Advanced Admin Features)
- [ ] **Import / Export Dữ liệu Người dùng & Báo cáo SLA**:
  - Tích hợp thư viện Apache POI: Cho phép Admin tải file mẫu Excel, điền thông tin và import hàng trăm tài khoản sinh viên/cán bộ cùng lúc.
  - Nút **"Xuất Báo Cáo SLA (.xlsx / .pdf)"** trên Dashboard để phục vụ họp giao ban trường và kiểm định chất lượng đào tạo.
- [ ] **Thao tác hàng loạt (Bulk Actions) trong Quản trị Người dùng**:
  - Tích hợp Checkbox chọn nhiều dòng, thanh công cụ nổi (Floating Action Bar): *Khóa tài khoản đã chọn*, *Gán Khoa/Phòng*, *Kích hoạt hàng loạt*.
- [ ] **Cấu hình SLA Đa Tầng theo Khoa / Phòng ban**:
  - Cho phép Admin thiết lập thời gian cam kết SLA riêng cho từng phòng ban (Ví dụ: Phòng Tuyển sinh 24h, Phòng Đào tạo 48h, Phòng CTSV 72h).

#### 🔹 Giai đoạn 3: Real-time hóa Kênh Chỉ Đạo Nội Bộ (WebSocket Modernization)
- [ ] Kết nối Kênh Trao Đổi Ban Quản Trị (`/admin/messages`) vào broker WebSocket STOMP (`/topic/internal-messages`), hỗ trợ nhận thông báo tin nhắn tức thời và đính kèm công văn PDF chỉ đạo nội bộ.


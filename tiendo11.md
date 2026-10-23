# 📋 TIẾN ĐỘ 11: KẾ HOẠCH NÂNG CẤP ADMIN DASHBOARD, PHÂN TRANG STAFF TICKET VÀ CƠ CHẾ SLA AUTO-ESCALATION

> **Trạng thái:** 🚀 Đang triển khai theo từng giai đoạn  
> **Phiên bản:** Version 2.0 (Bản nâng cấp quản trị & SLA toàn trường)  
> **Phân vai trách nhiệm:**  
> - **Dev A (Backend Lead & Database Architect):** Quản lý Seeder cleanup `race_staff`, Service phân trang `Page<TicketSummaryDto>`, Engine Escalation SLA, API & Controller endpoints.  
> - **Dev B (Frontend Lead & UI/UX Specialist):** Tối ưu 5 điểm UX/UI Admin Dashboard, ẩn Chatbot AI trên `/admin/*`, tích hợp Chart.js ma trận SLA và phân trang Thymeleaf.

---

## 🎯 I. TỔNG QUAN CÁC VẤN ĐỀ VÀ PHƯƠNG ÁN GIẢI QUYẾT

| STT | Phân hệ / Màn hình | Vấn đề hiện tại | Phương án giải quyết chuẩn Version 2 | Phụ trách |
| :---: | :--- | :--- | :--- | :---: |
| **1** | **Dữ liệu Database (Mock Staff)** | Tồn tại 10 tài khoản cán bộ test `race_staff_1` đến `race_staff_10` ("Cán bộ Test 10", race_staff_10@hcmute.edu.vn) gây rác danh sách quản trị. | Thêm cơ chế tự động dọn dẹp (clean-up) trong `DataInitializer.java` khi khởi động để xóa sạch vĩnh viễn các tài khoản `race_staff_%`. | **Dev A** |
| **2** | **Admin Dashboard UX/UI (Điểm 1 & 3)** | Trùng lặp 3 nút điều hướng ngay dưới tiêu đề; Thanh lọc thời gian & đơn vị bị dàn trải, nút "Đặt lại" bị trơ trọi. | - Xóa bỏ hoàn toàn 3 nút trùng lặp `[Trao Đổi...]`, `[Quản Trị Người Dùng]`, `[Danh Mục Khoa/Phòng]`.<br>- Gom 2 dropdown và nút đặt lại thành thanh ngang **Filter Bar** gắn liền, tiết kiệm chiều dọc. | **Dev B** |
| **3** | **Admin Dashboard Banner (Điểm 2)** | Nút `[📢 Đăng Thông Báo]` vàng cam chói gắt, không đúng trọng tâm trang giám sát SLA; Nút `[↗️ Xem Cổng Sinh Viên]` mờ nhạt. | Thay thế nút Đăng Thông Báo bằng `[📊 Xuất Báo Cáo SLA]` hoặc nút thanh lịch viền xanh/xám nhã nhặn, đồng bộ tông màu học thuật HCMUTE Navy. | **Dev B** |
| **4** | **Admin Dashboard KPI & Chatbot (Điểm 4)** | Chatbot AI góc dưới đè lên thẻ CSAT; Thanh tiến trình SLA 50% chưa có nền tương phản rõ rệt. | - Ẩn hoàn toàn widget Chatbot AI trên tất cả các trang `/admin/*`.<br>- Bổ sung background xám nhạt (`#e2e8f0`) cho thanh `progress-bar` để tương phản rõ rệt tỷ lệ % đạt chuẩn. | **Dev B** |
| **5** | **Admin SLA Analytics (Điểm 5)** | Thiếu biểu đồ trực quan & bảng tổng hợp giám sát nhanh; Admin khó nắm bắt đơn vị nào đang chậm trễ. | Tích hợp biểu đồ Chart.js phân bổ SLA theo Khoa/Phòng và Bảng Top Ticket Quá Hạn / Sắp Chạm Ngưỡng SLA để Admin can thiệp kịp thời. | **Dev A + B** |
| **6** | **Bàn Xử Lý Ticket Cán bộ (`/staff/tickets`)** | Danh sách Ticket hiển thị dạng danh sách phẳng (`List`), không có phân trang khi số lượng ticket tăng cao. | Bổ sung phân trang `Page<TicketSummaryDto>` với `Pageable` tại `StaffTicketWebController` và thanh điều hướng phân trang chuẩn Bootstrap 5 tại `staff-dashboard.html`. | **Dev A + B** |
| **7** | **Động Cơ Giám Sát SLA (Escalation Engine)** | Khi Ticket bị quá hạn xử lý (OVERDUE) hoàn toàn, chưa có cơ chế tự động chuyển giao về Admin Workspace để can thiệp. | Nâng cấp `SlaSchedulerService` & `TicketService`: Khi ticket vi phạm ngưỡng SLA nghiêm trọng, tự động gán cờ Escalated, mở quyền cho Admin điều phối lại hoặc xử lý trực tiếp. | **Dev A** |

---

## 🚀 II. KẾ HOẠCH TRIỂN KHAI THEO CÁC GIAI ĐOẠN

### 🔹 GIAI ĐOẠN 1: DỌN DẸP DỮ LIỆU MOCK CÁN BỘ (`race_staff_1` -> `race_staff_10`) [✅ HOÀN THÀNH]
- Đã thêm `cleanupMockRaceStaffUsers()` trong `DataInitializer.java`.

### 🔹 GIAI ĐOẠN 2: CHUẨN HÓA UI/UX ADMIN DASHBOARD (`/admin/dashboard`) [✅ HOÀN THÀNH]
- Đã xóa 3 nút trùng lặp, thanh filter bar gọn, ẩn chatbot trên admin, thêm Chart.js xu hướng SLA.

### 🔹 GIAI ĐOẠN 3: PHÂN TRANG BÀN XỬ LÝ TICKET CÁN BỘ (`/staff/tickets`) [✅ HOÀN THÀNH]
- Đã tích hợp `Pageable`, `Page<TicketSummaryDto>` và thanh điều hướng phân trang Bootstrap 5.

### 🔹 GIAI ĐOẠN 4: ĐỘNG CƠ SLA AUTO-ESCALATION & TAKEOVER [✅ HOÀN THÀNH]
- Đã tự động gán vé quá hạn tiếp nhận cho Admin và bổ sung nút Tiếp Quản khẩn cấp.

### 🔹 GIAI ĐOẠN 5: TINH GỌN DROPDOWN PROFILE ADMIN/STAFF & ĐỒNG BỘ TÊN NGƯỜI DÙNG TOÀN HỆ THỐNG [⏳ ĐANG TRIỂN KHAI]
- **Mục tiêu:** 
  1. Loại bỏ các mục sinh viên thừa ("Tổng quan học tập", "Bảng điểm học tập", "Thời khóa biểu", "Chương trình đào tạo", "Ticket của tôi") khỏi menu tài khoản của Admin và Staff. Với Admin/Staff chỉ giữ lại Thông tin cá nhân & Đăng xuất.
  2. Khắc phục lỗi hiển thị username `admin` thô ráp trên tab Bảng tin (`feed/official`). Sử dụng `userAccountMenu.displayName` và `@ModelAttribute("currentUserFullName")` để luôn hiển thị Họ tên thật ở 100% các trang.

### 🔹 GIAI ĐOẠN 6: KHẮC PHỤC LỖI THẺ HTML THÔ & TÍCH HỢP QUILL.JS CHO SOẠN THẢO VÀ NHẮN TIN TICKET [⏳ ĐANG TRIỂN KHAI]
- **Mục tiêu:**
  1. Nạp đầy đủ thư viện `quill.min.js` vào [create.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/ticket/create.html) để trình soạn thảo rich-text hoạt động trơn tru khi tạo yêu cầu.
  2. Trong [detail.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/ticket/detail.html), chuyển từ `th:text` sang `th:utext` cho `ticket.description` và tin nhắn trao đổi `h.actionNote`, loại bỏ hoàn toàn lỗi hiển thị các thẻ `<p>`, `<strong>`, `<br>` thô ráp.

### 🔹 GIAI ĐOẠN 7: TÁCH KÊNH TRAO ĐỔI BAN QUẢN TRỊ RA TAB RIÊNG BIỆT & NHẮN TIN REALTIME KHÔNG RELOAD TRANG [⏳ ĐANG TRIỂN KHAI]
- **Mục tiêu:**
  1. Tạo tab riêng **Kênh Trao Đổi Ban Quản Trị** trên `portal-header.html` (route `/admin/messages` hỗ trợ cả Staff và Admin).
  2. Xây dựng giao diện Chat toàn màn hình hiện đại tại `templates/admin/internal-messages.html`.
  3. Loại bỏ nút "Xuất Báo Cáo SLA" khỏi banner `portal-header.html`.
  4. Cơ chế trao đổi Real-time: Gửi nhận tin nhắn tức thì bằng AJAX fetch + WebSocket STOMP broker `/topic/internal-messages` + fallback 3s smart polling (tuyệt đối không yêu cầu người dùng phải reload trang mới thấy tin nhắn).

---

## 📝 III. TIÊU CHÍ HOÀN THÀNH (ACCEPTANCE CRITERIA)
- [x] Không còn bất kỳ tài khoản nào mang tiền tố `race_staff_` trong danh sách Người dùng (`/admin/users`).
- [x] Giao diện Admin Dashboard sạch sẽ, không còn nút trùng lặp, thanh filter bar thẳng hàng gọn gàng.
- [x] Không bị widget Chatbot AI che khuất thẻ CSAT trên `/admin/dashboard`.
- [x] Bàn làm việc cán bộ `/staff/tickets` có phân trang chuẩn mượt mà khi chuyển trang 1, 2, 3...
- [x] Ticket quá hạn SLA được tổng hợp rõ ràng và hỗ trợ Admin can thiệp trực tiếp (Auto-Escalation & Takeover).
- [x] Dropdown tài khoản Admin và Staff đã dọn sạch các mục sinh viên thừa, chỉ còn Thông tin cá nhân & Đăng xuất.
- [x] Tab Bảng tin hiển thị đúng Họ tên thật của người dùng thay vì chữ "admin".
- [x] Soạn ticket và tin nhắn trao đổi render định dạng HTML chuẩn (dùng `th:utext`), có Quill.js đầy đủ không bị lộ thẻ `<p>`.
- [x] Bỏ nút "Xuất Báo Cáo SLA"; Kênh trao đổi Ban Quản Trị là tab riêng trên thanh sub-nav với chat realtime không reload trang.
- [x] Toàn bộ mã nguồn vượt qua `mvn test-compile` không có bất kỳ lỗi cú pháp nào.

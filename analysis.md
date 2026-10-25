# BÁO CÁO PHÂN TÍCH KIẾN TRÚC & KẾ HOẠCH ĐỒNG BỘ GIAO DIỆN QAUTE PORTAL (BẢN 2)

**Dự án:** QAUTE Portal - Cổng Tư vấn Sinh viên & Hỗ trợ Học vụ HCMUTE (Version 2)  
**Tài liệu:** Kế hoạch kế thừa, chuyển giao và chuẩn hóa Header/Navigation Bar  
**Mã nguồn đối chiếu Bản 1:** `D:\HK5\LapTrinhWeb\doancuoiki\doancuoiki_demo1`  
**Mã nguồn triển khai Bản 2:** `D:\HK5\LapTrinhWeb\doancuoiki\ban2`  

---

## 1. XÁC NHẬN BỐI CẢNH & NGUYÊN TẮC KẾ THỪA (LEAD ARCHITECT)

1. **Backend & Cơ sở dữ liệu đã tương thích 100%:**
   - Đã kiểm tra toàn bộ tầng Controller, Service, Repository, DTO và Entity của Bản 2.
   - Các API và cơ chế lõi đều đang vận hành đúng chuẩn:
     * Atomic Query Claim Ticket (`@Modifying UPDATE Ticket t SET ... WHERE t.status = 'OPEN'`) chống triệt để Race Condition.
     * Cờ `hasStudentReply = true` cảnh báo tin mới từ Sinh viên kèm API đếm realtime `/api/v1/staff/tickets/unread-summary`.
     * Tự động suy diễn mức độ ưu tiên và thời hạn phản hồi SLA (24h - 120h) từ danh mục 15 thủ tục học vụ (`TicketCategoryEnum`).
     * Toàn bộ các route backend cho Sinh viên (`/tickets/my-tickets`), Cán bộ (`/staff/tickets`), Quản trị (`/admin/dashboard`), Trò chuyện AI (`/ai-chat`), Học vụ (`/academic/sync`, `/academic/grades`, `/academic/timetable`) đã sẵn sàng phục vụ.
   - **Cam kết:** Giữ nguyên vẹn 100% logic Backend Java, không tự ý viết lại hay thay đổi các class lõi.

2. **Chuyển giao UI/UX từ Bản 1:**
   - Thân trang (Main Content Canvas), bảng dữ liệu, các modal, CSS token và các đoạn script Poller/WebSocket của các trang nghiệp vụ đã hoàn thiện ở Bản 1 được **bê nguyên vẹn sang Bản 2**.
   - `ticket/detail.html`: File hiện tại ở Bản 2 đang là bản Mock tĩnh (10 KB). Đã định vị được file gốc hoàn chỉnh (162 KB) tại `D:\HK5\LapTrinhWeb\doancuoiki\doancuoiki_demo1\src\main\resources\templates\ticket\detail.html` với bố cục **Viewport Docked Chat**, hỗ trợ gửi tệp đính kèm độc lập, Lightbox preview, danh thiếp Cán bộ và Modal CSAT.

---

## 2. MA TRẬN ĐIỀU HƯỚNG & ĐỒNG BỘ HEADER (NAVBAR)

Header hệ thống sẽ được tập trung tinh chỉnh tại file:
`src/main/resources/templates/layout/navbar.html`

### A. Nhận diện thương hiệu HCMUTE
- Màu chủ đạo: **Navy Blue** (`#003882` / `#1E3A8A`) kết hợp điểm nhấn **Crimson** (`#991B1E` / `#A51C30`).
- Logo HCMUTE đặt ở góc trái, liên kết về trang chủ `/`.

### B. Menu điều hướng chính (Main Navigation Links)
| Tên Menu | Biểu tượng (Icon) | Đường dẫn (`th:href`) | Đối tượng hiển thị | Ghi chú kỹ thuật |
| :--- | :--- | :--- | :--- | :--- |
| **Trang Chủ** | `bi-house-door` | `@{/}` | Tất cả người dùng | Active khi path = `/` |
| **Diễn Đàn / Bảng Tin** | `bi-chat-left-dots` | `@{/feed/forum}` | Tất cả người dùng | Có thể mở dropdown hoặc dẫn trực tiếp Diễn đàn |
| **Hỗ Trợ AI** | `bi-robot` / `bi-stars` | `@{/ai-chat}` | Tất cả người dùng | Trỏ về controller `HomeController.aiChatPage` |
| **Tra Cứu FAQ** | `bi-search` | `@{/faqs}` | Tất cả người dùng | Tìm kiếm câu hỏi thường gặp |
| **Ticket Hỗ Trợ** | `bi-ticket-perforated` | Theo phân quyền: <br>• SV: `@{/tickets/my-tickets}`<br>• Cán bộ: `@{/staff/tickets}`<br>• Admin: `@{/admin/dashboard}` | Phụ thuộc Role | Tự động phân luồng theo quyền hạn người dùng |

### C. Menu Nghiệp Vụ Cán Bộ & Quản Trị (Desk Dropdown)
Hiển thị với tag `sec:authorize="hasAnyRole('STAFF', 'ADMIN')"`:
- **Bàn Xử Lý Ticket:** `@{/staff/tickets}` (kèm badge đếm realtime `hasStudentReply`).
- **Duyệt Bài Diễn Đàn:** `@{/moderation/posts}`.
- **Đăng Thông Báo Mới:** `@{/feed/official/create}`.
- **Quản Trị Hệ Thống (Chỉ Admin):**
  * SLA Dashboard Toàn Trường: `@{/admin/dashboard}`.
  * Quản Lý Người Dùng: `@{/admin/users}`.
  * Quản Lý Khoa / Phòng Ban: `@{/admin/departments}`.

### D. Dropdown Tài Khoản Người Dùng (Authenticated User Dropdown)
Hiển thị khi đăng nhập `sec:authorize="isAuthenticated()"`:
1. **Thông tin cá nhân:** `@{/profile}` (Hồ sơ, đổi mật khẩu, cấu hình portal).
2. **Tổng quan học tập:** `@{/academic/sync}` (Đồng bộ tín chỉ, tiến độ tốt nghiệp).
3. **Bảng điểm học tập:** `@{/academic/grades}` (Chi tiết điểm thi các học kỳ).
4. **Lịch học & Lịch thi:** `@{/academic/timetable}` (Thời khóa biểu tuần/kỳ).
5. **Ticket của tôi:** `@{/tickets/my-tickets}` (Dành cho sinh viên).
6. **Đăng xuất:** `POST @{/auth/logout}` (Xử lý CSRF Spring Security).

### E. Người dùng Khách vãng lai (`sec:authorize="!isAuthenticated()"`)
- Nút **Đăng Nhập**: `@{/auth/login}`.
- Nút **Đăng Ký**: `@{/auth/register}`.

### F. Mobile Responsive Navigation
- Nút Menu Hamburger bật tắt Drawer/Collapse trên thiết bị di động (`xl:hidden` / `navbar-toggler`).
- Hiển thị đầy đủ cây thư mục điều hướng khi thu nhỏ màn hình điện thoại/tablet mà không bị vỡ giao diện.

---

## 3. PHẠM VI BẢO VỆ TUYỆT ĐỐI & DANH SÁCH FILE THỰC HIỆN

### ⛔ KHU VỰC CẤM CAN THIỆP (GIỮ NGUYÊN 100% CỦA BẢN 2)
Các phân hệ dưới đây ở Bản 2 đã được xây dựng riêng rất tốt và có tính năng crawler đồng bộ dữ liệu sinh viên trực tiếp từ Cổng đào tạo HCMUTE. **Tuyệt đối KHÔNG đụng vào, không sửa đổi hay ghi đè:**
1. **`templates/profile.html`**: Giao diện Thông tin cá nhân, hồ sơ sinh viên, thông tin liên lạc, đổi mật khẩu.
2. **`templates/academic-sync.html`**: Giao diện Tổng quan học tập, công cụ Crawler tự động đồng bộ điểm, tín chỉ từ Portal HCMUTE.
3. **`templates/grades.html`**: Bảng điểm học tập chi tiết theo từng học kỳ.
4. **`templates/timetable.html`**: Lịch học và thời khóa biểu sinh viên.
5. **Toàn bộ Backend Học vụ**: `HcmuteCrawlerService`, `AcademicWebController`, `AcademicSyncApiController`, `ProfileService`.
6. **Các màn hình Ticket đã tối ưu ở Bản 2**:
   - `templates/ticket/create.html`: Đã tích hợp Quill Editor, preview file và tự động ẩn SLA/Priority.
   - `templates/ticket/my-tickets.html`: Đã chuẩn hóa Spring Data Pagination, ẩn cột SLA với sinh viên.

---

### 🎯 CHÍNH XÁC CÁC GIAO DIỆN CẦN CHỈNH SỬA & CHUYỂN GIAO:

Chỉ thực hiện đúng 2 file mục tiêu duy nhất:

#### 1. `templates/layout/navbar.html` (Header / Thanh điều hướng):
- **Mục đích:** Đồng bộ màu nhận diện (Navy `#003882`, Crimson `#991b1e`), bổ sung dải menu chính và liên kết tới các tính năng xịn sò sẵn có của Bản 2.
- **Các điểm điều chỉnh:**
  * Menu chính: [Trang chủ] (`/`), [Diễn đàn / Bảng tin] (`/feed/forum`), [Hỗ trợ AI] (`/ai-chat`), [Tra cứu FAQ] (`/faqs`), [Ticket hỗ trợ] (SV: `/tickets/my-tickets`, Cán bộ: `/staff/tickets`).
  * Dropdown người dùng: Giữ nguyên các trang của Bản 2 và trỏ link chính xác vào đó:
    - Hồ sơ cá nhân $\rightarrow$ `th:href="@{/profile}"` (View Bản 2 giữ nguyên)
    - Tổng quan học tập $\rightarrow$ `th:href="@{/academic/sync}"` (View Bản 2 giữ nguyên)
    - Bảng điểm $\rightarrow$ `th:href="@{/academic/grades}"` (View Bản 2 giữ nguyên)
    - Lịch học $\rightarrow$ `th:href="@{/academic/timetable}"` (View Bản 2 giữ nguyên)
    - Đăng xuất $\rightarrow$ `POST @{/auth/logout}`
  * Drawer Mobile Responsive: Nút bật tắt Drawer trên thiết bị di động (`xl:hidden`).

#### 2. `templates/ticket/detail.html` (Chi tiết vé trao đổi 2 chiều):
- **Mục đích:** Thay thế file mock tĩnh hiện tại (10 KB) bằng bản Viewport Docked Chat hoàn chỉnh (162 KB) từ `doancuoiki_demo1`.
- **Nội dung kế thừa:** Stream tin nhắn cuộn, thanh dock nhập liệu ghim đáy màn hình, gửi tệp đính kèm độc lập, Lightbox preview, danh thiếp cán bộ phụ trách, modal CSAT.


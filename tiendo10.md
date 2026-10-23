# 📋 TIẾN ĐỘ 10: XÁC NHẬN BẢN THIẾT KẾ GIAO DIỆN & KẾ HOẠCH TÁI CẤU TRÚC PHÂN HỆ TICKET
> **Trạng thái:** ⏳ Đang chờ phê duyệt từ Dev Lead (CHƯA CODE BẤT KỲ FILE NÀO)  
> **Ngày lập kế hoạch:** 23/10/2026  
> **Tài liệu phân tích kỹ thuật:** [`docs/analysis/2026-10-23_ticket_restructure_sla_pagination_analysis.md`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/docs/analysis/2026-10-23_ticket_restructure_sla_pagination_analysis.md)

---

## 📌 1. TỔNG QUAN VẤN ĐỀ CẦN TÁI CẤU TRÚC

| STT | Vấn đề hiện tại | Giải pháp thiết kế mới | Tác động |
| :---: | :--- | :--- | :--- |
| **1** | Sinh viên tự chọn `Priority` dẫn đến 100% ticket bị đẩy lên `URGENT`. | Hệ thống tự động suy diễn `Priority` và `SLA` từ **Loại đơn/Thủ tục** (`TicketCategoryEnum`). | Loại bỏ lỗ hổng phân quyền, chuẩn hóa cam kết SLA công bằng. |
| **2** | Tồn tại thanh giả lập "Mock State Switcher" và hàng trăm dòng TH2 dữ liệu tĩnh. | Xóa bỏ 100% thanh Switcher và dữ liệu mock. Chỉ render dữ liệu thực từ `Page<TicketResponseDto>`. | Codebase sạch 100%, không còn rác debug/demo. |
| **3** | Phân trang JS giả lập, không gửi HTTP query request lên server. | Tích hợp phân trang Spring Boot chuẩn (`th:with="totalPages=${tickets.totalPages}"`, bảo toàn URL query params). | Hỗ trợ phân trang chuẩn, tìm kiếm và lọc động mượt mà. |
| **4** | Cột SLA chưa trực quan theo hạn chót thực tế. | Hiển thị Ngày tạo, Hạn xử lý `dueDate` kèm nhãn cảnh báo thời gian thực: `Quá hạn SLA` (Đỏ), `Sắp hết hạn` (Vàng), `Đúng hạn` (Xanh). | Sinh viên & Chuyên viên nắm bắt tiến độ chính xác. |

---

## 🎨 2. MÔ TẢ CHI TIẾT GIAO DIỆN MỚI (`my-tickets.html`)

### 2.1. Bản vẽ Wireframe / Mockup Giao diện Mới (ASCII Layout)

```text
+---------------------------------------------------------------------------------------------------------+
| [LOGO HCMUTE]   Trang chủ   Bảng tin   Diễn đàn   Hỗ trợ AI   Tra cứu FAQ   [Ticket hỗ trợ]   (User Profile)|
+---------------------------------------------------------------------------------------------------------+
|                                                                                                         |
| Trang chủ / Dịch vụ sinh viên / Ticket hỗ trợ                                                            |
|                                                                                                         |
| TICKET HỖ TRỢ CỦA TÔI  [ 12 yêu cầu ]                                    [ Tải lại ]  [ + Tạo ticket mới]|
| Theo dõi tiến độ giải quyết và cam kết thời gian xử lý (SLA) từ các Khoa/Phòng                           |
|                                                                                                         |
| +-----------------------------------------------------------------------------------------------------+ |
| | [ KPI 1: Tổng số ]      [ KPI 2: Chờ tiếp nhận ]    [ KPI 3: Đang xử lý ]    [ KPI 4: Đã giải quyết ]| |
| | 12 ticket (Tổng)        3 ticket (Cần xử lý)        4 ticket (Tiến trình)    5 ticket (Hoàn tất)     | |
| +-----------------------------------------------------------------------------------------------------+ |
|                                                                                                         |
| [ ĐÃ XÓA HOÀN TOÀN THANH DEMO MOCK STATE VIEW SWITCHER TẠI ĐÂY ]                                        |
|                                                                                                         |
| +--- KHỐI BỘ LỌC ĐA NĂNG (FILTER TOOLBAR) -------------------------------------------------------------+ |
| | [Tất cả (12)]  [Chờ tiếp nhận (3)]  [Đang xử lý (4)]  [Chờ phản hồi (1)]  [Đã giải quyết (5)]  [Đã đóng] | |
| |-----------------------------------------------------------------------------------------------------| |
| | [🔍 Tìm theo mã Ticket (TKT-...) hoặc tiêu đề...]  [Tất cả Khoa/Phòng ▼]  [Lọc]  [↺ Xóa bộ lọc]     | |
| +-----------------------------------------------------------------------------------------------------+ |
|                                                                                                         |
| +--- BẢNG DỮ LIỆU TICKET (6 CỘT CHUẨN SAAS HCMUTE) ----------------------------------------------------+ |
| | MÃ & TIÊU ĐỀ THỦ TỤC       | ĐƠN VỊ TIẾP NHẬN | TRẠNG THÁI     | ĐỘ ƯU TIÊN  | HẠN XỬ LÝ (SLA) | THAO TÁC | |
| |----------------------------|------------------|----------------|-------------|-----------------|----------| |
| | TKT-2026-00128             | Phòng Đào tạo    | [Đang xử lý]   | [● Khẩn cấp]| Hạn: 24/10 14:00| [👁 Xem] | |
| | Đơn xin hoãn thi cuối kỳ   | (Học vụ & ĐT)    |                | (Hệ thống)  | (⏳ Sắp hết hạn)|          | |
| | [Thủ tục: Hoãn thi]        |                  |                |             |                 |          | |
| |----------------------------|------------------|----------------|-------------|-----------------|----------| |
| | TKT-2026-00125             | TT Công nghệ TT  | [Chờ phản hồi] | [● Cao]     | Hạn: 23/10 09:00| [👁 Xem] | |
| | Mở khóa tài khoản LMS sinh | (Kỹ thuật CNTT)  |                | (Hệ thống)  | (⚠️ Quá hạn SLA)|          | |
| | [Thủ tục: Khóa LMS]        |                  |                |             |                 |          | |
| |----------------------------|------------------|----------------|-------------|-----------------|----------| |
| | TKT-2026-00120             | Phòng CTSV       | [Đã giải quyết]| [● Thấp]    | Giải quyết lúc: | [👁 Xem] | |
| | Xin cấp giấy hoãn NVQS     | (CT Sinh viên)   |                | (Hệ thống)  | 21/10/2026 15:30|          | |
| | [Thủ tục: Giấy tờ CTSV]    |                  |                |             | (✓ Đúng hạn)    |          | |
| +-----------------------------------------------------------------------------------------------------+ |
|                                                                                                         |
| +--- PHÂN TRANG SPRING BOOT CHUẨN DATA (PAGINATION) ---------------------------------------------------+ |
| | Hiển thị 1 - 10 trong tổng số 12 ticket                           [« Trước]  [ 1 ]  [ 2 ]  [Sau »]   | |
| +-----------------------------------------------------------------------------------------------------+ |
|                                                                                                         |
| (NẾU DANH SÁCH RỖNG - EMPTY STATE):                                                                    |
| +-----------------------------------------------------------------------------------------------------+ |
| |                                 [ Icon Thư mục trống ]                                              | |
| |                             Chưa tìm thấy yêu cầu hỗ trợ nào                                         | |
| |      Bạn hiện tại không có ticket nào thỏa mãn điều kiện lọc hoặc chưa từng gửi yêu cầu tư vấn.      | |
| |                                   [ + Gửi Ticket Hỗ Trợ Ngay ]                                      | |
| +-----------------------------------------------------------------------------------------------------+ |
```

---

## 🔍 3. CHI TIẾT THÀNH PHẦN GIAO DIỆN ĐỔI MỚI

### 3.1. Header & Thẻ Chỉ Số KPI
- **Breadcrumb:** `Trang chủ` / `Dịch vụ sinh viên` / `Ticket hỗ trợ`.
- **Tiêu đề chính:** `Ticket hỗ trợ` kèm badge đếm tổng số lượng vé.
- **Nút hành động:**
  - `Tải lại`: Làm mới trang và đồng bộ lại danh sách.
  - `Tạo ticket mới`: Nút xanh thương hiệu HCMUTE dẫn đến `/tickets/create`.
- **4 Thẻ KPI Cards:**
  - `Tất cả ticket` (Viền xanh dương đậm `#003882`)
  - `Chờ tiếp nhận` (Viền vàng hổ phách `amber-500`)
  - `Đang xử lý` (Viền xanh da trời `sky-500`)
  - `Đã giải quyết` (Viền xanh lá cây `emerald-500`)
  - Khi click vào từng thẻ KPI $\rightarrow$ tự động kích hoạt lọc tương ứng.

### 3.2. Khối Bộ Lọc Đa Năng (Filter Toolbar)
- **Hàng Tab Trạng thái (Status Tabs):**
  - Gồm 6 tab: `Tất cả`, `Chờ tiếp nhận`, `Đang xử lý`, `Chờ phản hồi`, `Đã giải quyết`, `Đã đóng`.
  - Mỗi tab hiển thị số lượng vé thật đếm từ Backend (`statusCounts[status]`).
  - Active tab có nền `bg-[#eff6ff]` và chữ xanh navy `text-[#003882]`, viền nổi bật.
- **Hàng Điều khiển Tìm kiếm & Dropdown:**
  - `Ô tìm kiếm`: Placeholder `"Tìm theo mã Ticket (VD: TKT-2026...) hoặc tiêu đề..."`, hỗ trợ bấm Enter hoặc nút Tìm kiếm để gửi query param `?keyword=...`.
  - `Dropdown Khoa/Phòng`: Lấy dữ liệu thật từ `departments` trong database.
  - `Nút Lọc`: Thực thi gửi form GET request bảo toàn các tham số.
  - `Nút Xóa bộ lọc`: Reset tất cả param về mặc định (`/tickets/my-tickets`).

### 3.3. Bảng Dữ Liệu Ticket 6 Cột Chuẩn
1. **Cột 1: Tiêu đề & Loại thủ tục (35% độ rộng):**
   - Mã Ticket font Mono in hoa, màu xanh navy (`TKT-2026-00128`).
   - Tiêu đề ticket: Chữ đậm, hover đổi màu, click mở chi tiết vé.
   - Nhãn Loại thủ tục học vụ (VD: `Đơn xin hoãn thi`, `Khóa LMS`, `Cấp bảng điểm`).
2. **Cột 2: Đơn vị tiếp nhận (15%):**
   - Tên Khoa/Phòng phụ trách (Phòng Đào tạo, Phòng CTSV, TT CNTT...).
3. **Cột 3: Trạng thái tiếp nhận (15%):**
   - Badge màu học đường:
     - `OPEN` / `PENDING`: Badge xám `Chờ tiếp nhận`.
     - `IN_PROGRESS`: Badge xanh dương `Đang xử lý` kèm icon xoay nhẹ.
     - `WAITING_FOR_STUDENT`: Badge vàng cam `Chờ phản hồi` (cần sinh viên bổ sung thông tin).
     - `RESOLVED`: Badge xanh ngọc `Đã giải quyết`.
     - `CLOSED`: Badge xám tro `Đã đóng`.
4. **Cột 4: Mức độ ưu tiên (Read-only Badge) (12%):**
   - Do hệ thống tự xác định từ loại thủ tục:
     - `URGENT`: Nền đỏ hồng phấn `bg-rose-50`, chữ `text-rose-700`, chấm đỏ nhấp nháy (`animate-pulse`).
     - `HIGH`: Nền cam nhạt `bg-orange-50`, chữ `text-orange-700`.
     - `MEDIUM`: Nền hổ phách `bg-amber-50`, chữ `text-amber-700`.
     - `LOW`: Nền xám `bg-slate-100`, chữ `text-slate-600`.
5. **Cột 5: Hạn xử lý (SLA) & Đếm ngược (15%):**
   - Dòng 1: Ngày tạo vé định dạng `dd/MM/yyyy HH:mm`.
   - Dòng 2: Hạn chót `dueDate` kèm nhãn cảnh báo:
     - `Quá hạn SLA`: Nền đỏ chữ trắng nếu hiện tại > `dueDate` và vé chưa đóng.
     - `Sắp hết hạn`: Nền vàng chữ nâu nếu còn dưới 12h.
     - `Đúng hạn`: Nền xanh nhạt chữ xanh lá nếu vẫn trong thời hạn xử lý.
6. **Cột 6: Thao tác (8%):**
   - Nút icon `visibility` Xem chi tiết dẫn thẳng tới `/tickets/detail/{id}`.

### 3.4. Phân trang Spring Boot & Trạng thái Rỗng
- **Thanh phân trang chuẩn:**
  - Bên trái: `Hiển thị [start] – [end] trong tổng số [totalElements] ticket`.
  - Bên phải: Cụm nút `Trước`, số trang `1, 2, 3...`, `Sau`.
  - URL giữ nguyên các filter: `th:href="@{/tickets/my-tickets(page=${i}, status=${currentStatus}, departmentId=${selectedDept}, keyword=${keyword})}"`.
- **Empty State:**
  - Khi không có dữ liệu thật: Hiển thị minh họa hộp rỗng trang nhã, dòng thông điệp hướng dẫn rõ ràng và nút bấm `Tạo ticket mới`.

---

## ⚙️ 4. KẾ HOẠCH BACKEND TRIỂN KHAI SAU KHI ĐƯỢC DUYỆT

1. **Tạo Enum `TicketCategoryEnum`:**
   - Định nghĩa các hằng số: `DEFER_EXAM`, `ACADEMIC_SUSPENSION`, `DROPOUT_REQUEST`, `GRADUATION_APPEAL`, `LMS_PORTAL_LOCK`, `COURSE_REGISTRATION_ISSUE`, `TUITION_RECONCILIATION`, `TRANSCRIPT_REQUEST`, `PROFILE_UPDATE`...
   - Mỗi hằng số chứa: `vietnameseName`, `priority` (URGENT/HIGH/MEDIUM/LOW), `slaHours` (24h/48h/72h/120h), `defaultDepartmentCode`.
2. **Nâng cấp `SlaCalculatorService`:**
   - Thêm phương thức `calculateDueDateByCategory(TicketCategoryEnum category)` và `determinePriority(TicketCategoryEnum category)`.
   - Logic an toàn: Sinh viên gửi request tạo ticket chỉ cần truyền `categoryCode`, server tự gán `priority` và `dueDate`.
3. **Cập nhật `TicketWebController.myTicketsPage`:**
   - Tiếp nhận tham số:
     ```java
     @GetMapping({"", "/", "/my-tickets", "/list"})
     public String myTicketsPage(
         @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
         @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
         @RequestParam(name = "departmentId", required = false) Long departmentId,
         @RequestParam(name = "keyword", required = false) String keyword,
         Model model
     )
     ```
   - Gọi `ticketService.getMyTicketsPaged(currentUserId, status, departmentId, keyword, pageable)` trả về `Page<TicketResponseDto>`.
   - Đếm số lượng theo trạng thái nạp vào `Map<String, Long> statusCounts`.
4. **Refactor toàn diện file `my-tickets.html`:**
   - Thay thế toàn bộ mã mock bằng mã Thymeleaf tinh gọn, sạch đẹp, đúng chuẩn kiến trúc.

---

## 🧹 6. PHƯƠNG ÁN XÓA CỨNG DỮ LIỆU TICKET CŨ & KHỞI TẠO SEED DATA MỚI ĐỂ TEST

### 6.1. Đánh giá An toàn Cơ sở dữ liệu (Safety Assessment)
- **Mục tiêu:** Xóa sạch toàn bộ ticket cũ rác từ đồ án trước để tiện test hệ thống mới, đảm bảo **không làm hỏng project**.
- **Phân tích quan hệ khóa ngoại (Foreign Key Dependency):**
  - Bảng bị xóa:
    1. `ticket_histories` (Bảng con, FK `ticket_id` $\rightarrow$ `tickets.id`): Xóa sạch trước.
    2. `attachments` (Các file đính kèm của ticket, `WHERE ticket_id IS NOT NULL`): Xóa các bản ghi liên quan ticket.
    3. `tickets` (Bảng chính): Xóa sạch toàn bộ sau khi đã xóa bảng con.
  - **Các bảng hệ thống TUYỆT ĐỐI KHÔNG ĐỤNG TỚI:**
    - `users`, `roles`, `user_roles`: Toàn bộ tài khoản sinh viên, cán bộ, admin giữ nguyên vẹn 100%.
    - `departments`: Giữ nguyên danh mục 6 Khoa/Phòng ban.
    - `posts`, `post_comments`, `post_reactions`, `post_reports`: Giữ nguyên dữ liệu Bảng tin & Diễn đàn.
    - `faqs`: Giữ nguyên 91 câu hỏi FAQ và vector embeddings.
    - `otp_tokens`: Giữ nguyên lịch sử xác thực.
- **Kết luận:** Phương án an toàn tuyệt đối, hệ thống hoạt động bình thường, không gây lỗi ràng buộc quan hệ toàn vẹn.

### 6.2. Câu lệnh SQL Xóa Cứng Chuẩn (Thực thi khi được duyệt)
```sql
-- 1. Xóa các tệp đính kèm liên quan đến ticket
DELETE FROM attachments WHERE ticket_id IS NOT NULL;

-- 2. Xóa toàn bộ lịch sử trao đổi và tiến trình xử lý vé
DELETE FROM ticket_histories;

-- 3. Xóa toàn bộ dữ liệu bảng ticket cũ
DELETE FROM tickets;

-- 4. Reset chỉ số tự tăng (Auto Increment) về 1 để mã ticket sinh ra đẹp từ đầu
ALTER TABLE tickets AUTO_INCREMENT = 1;
ALTER TABLE ticket_histories AUTO_INCREMENT = 1;
```

### 6.3. Thiết kế Bộ Seed Data Mới Chuẩn Nghiệp Vụ SLA (7 Vé Mẫu)
Sau khi xóa sạch dữ liệu cũ, hệ thống sẽ tự động khởi tạo 7 Ticket mẫu trong `DataInitializer.java` gán cho tài khoản sinh viên đang test (`student@hcmute.edu.vn` và tài khoản sinh viên Cổng UTE), bao phủ toàn diện 100% kịch bản kiểm thử:

| STT | Mã Ticket | Loại thủ tục (`TicketCategoryEnum`) | Khoa/Phòng tiếp nhận | Trạng thái (`Status`) | Ưu tiên (`Priority`) | Tình trạng SLA | Mục đích kiểm thử |
| :---: | :---: | :--- | :--- | :---: | :---: | :---: | :--- |
| **1** | `TKT-2026-00001` | `DEFER_EXAM`<br>(Đơn xin hoãn thi) | Phòng Đào tạo & Khảo thí | `OPEN`<br>(Chờ tiếp nhận) | **URGENT**<br>(SLA 24h) | 🔴 **Quá hạn SLA**<br>(Tạo cách đây 36h, lố 12h) | Kiểm tra hiển thị Badge cảnh báo Đỏ `Quá hạn SLA` trên bảng. |
| **2** | `TKT-2026-00002` | `ACADEMIC_SUSPENSION`<br>(Tạm dừng/Bảo lưu kỳ) | Phòng Đào tạo & CTSV | `IN_PROGRESS`<br>(Đang xử lý) | **URGENT**<br>(SLA 24h) | 🟡 **Sắp hết hạn**<br>(Tạo cách đây 18h, còn 6h) | Kiểm tra nhãn Vàng `Sắp hết hạn` (< 12h) và tên chuyên viên xử lý. |
| **3** | `TKT-2026-00003` | `LMS_PORTAL_LOCK`<br>(Mở khóa tài khoản LMS) | Khoa CNTT & TT CNTT | `IN_PROGRESS`<br>(Đang xử lý) | **HIGH**<br>(SLA 48h) | 🟢 **Đúng hạn**<br>(Tạo cách đây 4h, còn 44h) | Kiểm tra ưu tiên Cao và tiến trình đang xử lý đúng hạn. |
| **4** | `TKT-2026-00004` | `COURSE_REGISTRATION_ISSUE`<br>(Lỗi ĐKHP tốt nghiệp) | Phòng Đào tạo | `WAITING_FOR_STUDENT`<br>(Chờ phản hồi) | **HIGH**<br>(SLA 48h) | 🟢 **Đúng hạn**<br>(Chờ SV bổ sung ảnh lỗi) | Kiểm tra Tab `Chờ phản hồi`, cờ `hasStudentReply = false`. |
| **5** | `TKT-2026-00005` | `TRANSCRIPT_REQUEST`<br>(Trích lục bảng điểm) | Phòng Đào tạo | `RESOLVED`<br>(Đã giải quyết) | **MEDIUM**<br>(SLA 72h) | 🟢 **Đúng hạn**<br>(Có `resolvedAt`) | Kiểm tra Tab `Đã giải quyết`, hiển thị ngày giờ hoàn tất. |
| **6** | `TKT-2026-00006` | `PROFILE_UPDATE`<br>(Cập nhật CCCD mới) | Phòng Đào tạo & CTSV | `CLOSED`<br>(Đã đóng hồ sơ) | **LOW**<br>(SLA 120h) | ⚪ **Đã hoàn thành**<br>(Có `closedAt`) | Kiểm tra Tab `Đã đóng`, độ ưu tiên Thấp (120h). |
| **7** | `TKT-2026-00007` | `TUITION_RECONCILIATION`<br>(Đối soát trừ trùng học phí) | Phòng Kế hoạch Tài chính | `OPEN`<br>(Chờ tiếp nhận) | **MEDIUM**<br>(SLA 72h) | 🟢 **Đúng hạn**<br>(Mới tạo 1h trước) | Kiểm tra chức năng lọc theo đơn vị `Kế hoạch Tài chính`. |

---

## ✋ 7. XÁC NHẬN CHỜ DUYỆT TỪ DEV LEAD
- [x] Đã hoàn thành lập tài liệu phân tích kỹ thuật: [`docs/analysis/2026-10-23_ticket_restructure_sla_pagination_analysis.md`](file:///d:/HK5/LapTrinhWeb/doancuoiki\ban2\docs\analysis\2026-10-23_ticket_restructure_sla_pagination_analysis.md).
- [x] Đã thiết kế chi tiết giao diện mới vào file: `tiendo10.md`.
- [x] Đã lên phương án xóa sạch dữ liệu cũ và thiết kế 7 Ticket mẫu chuẩn test.
- [x] **TUÂN THỦ LỆNH NGHIÊM NGẶT: CHƯA CODE GÌ HẾT, CHƯA CHẠY LỆNH XÓA DATABASE.**

👉 **Xin mời bạn xem xét phương án làm mới dữ liệu & giao diện trên. Khi bạn duyệt và ra lệnh "Duyệt" hoặc "Tiến hành code", AI sẽ lập tức thực thi an toàn từng bước!**

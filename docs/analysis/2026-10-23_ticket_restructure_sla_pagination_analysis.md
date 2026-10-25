# BÁO CÁO PHÂN TÍCH & KẾ HOẠCH TÁI CẤU TRÚC PHÂN HỆ TICKET VÀ SLA ENGINE
**Ngày lập báo cáo:** 23/10/2026  
**Người phụ trách:** Dev A (Backend Lead & Database Architect)  
**Dự án:** HCMUTE Support Portal (Cổng Dịch vụ & Tư vấn Sinh viên SPKT)  
**Phân hệ:** Quản lý Ticket Hỗ Trợ, SLA Engine & Phân trang Spring Data JPA  

---

## 1. BỐI CẢNH & PHÂN TÍCH HIỆN TRẠNG (AS-IS ANALYSIS)

### 1.1. Lỗ hổng phân quyền & Nghiệp vụ Mức độ ưu tiên (Priority) và SLA
- **Thực trạng:**
  - Form tạo Ticket và logic xử lý đang cho phép người dùng (sinh viên, khách vãng lai) tự chỉ định trường `priority` (`URGENT`, `HIGH`, `MEDIUM`, `LOW`).
  - Hệ quả vận hành: Người dùng luôn có xu hướng chọn `URGENT` hoặc `HIGH` để yêu cầu của mình được bộ phận đào tạo xử lý trước, làm vô hiệu hóa hàng đợi ưu tiên và gây quá tải bất hợp lý cho chuyên viên xử lý.
  - Việc tính toán thời hạn SLA (`dueDate`) dựa trực tiếp vào tham số `priority` do người dùng tự gửi lên, không phản ánh tính chất thực tế của loại thủ tục học vụ.
- **Rủi ro:**
  - Vi phạm nguyên tắc bảo mật và công bằng nghiệp vụ.
  - Mất kiểm soát SLA cam kết (Service Level Agreement) của từng Khoa/Phòng chức năng.

### 1.2. Vấn đề giao diện thừa thãi & Mock data tĩnh trong Thymeleaf
- **Thực trạng:**
  - File `my-tickets.html` hiện chứa thanh **"Demo Mock State View Switcher"** (`#stateSwitcherBar`) với 4 nút chuyển đổi trạng thái giả lập (*Dữ liệu đầy đủ, Đang tải Skeleton, Không tìm thấy, Chưa có ticket nào*).
  - Tồn tại hai nhánh render song song:
    - **TH1:** Render qua `th:block th:if="${tickets != null and !tickets.isEmpty()}"` nhưng model chỉ truyền `List<TicketSummaryDto>` không phân trang.
    - **TH2:** Render dữ liệu giả lập (Mock data) viết cứng hàng trăm dòng HTML với các sự kiện JS `onclick="viewTicketDetail('TKT-...')"` chiếm phần lớn dung lượng file và làm rối luồng người dùng thật.
  - Phân trang hiển thị cứng, chạy bằng hàm JavaScript giả lập `switchPage(page)` phía client chứ không tương tác với Spring Data JPA.

### 1.3. Thiếu liên kết Phân trang chuẩn Backend (Spring Data Pageable)
- Endpoint `GET /tickets/my-tickets` trong `TicketWebController`:
  - Chưa tiếp nhận `Pageable pageable`, `status`, `departmentId`, `keyword`.
  - Chưa hỗ trợ Dynamic Query lọc dữ liệu từ cơ sở dữ liệu MySQL mà trả về toàn bộ danh sách, gây chậm chạp khi sinh viên có nhiều ticket và không hỗ trợ bookmark/chia sẻ liên kết URL có query param (`?page=0&size=10&status=OPEN&keyword=...`).

---

## 2. THIẾT KẾ KIẾN TRÚC & GIẢI PHÁP TÁI CẤU TRÚC (TO-BE ARCHITECTURE)

### 2.1. Ma trận Phân loại Thủ tục Học vụ & Tự động Suy diễn SLA (`TicketCategoryEnum`)
Hệ thống loại bỏ hoàn toàn quyền chọn `priority` từ phía giao diện của sinh viên. Mức độ ưu tiên và thời hạn cam kết SLA được xác định tự động 100% dựa vào **Loại đơn / Thủ tục học vụ** (`TicketCategoryEnum`):

| Nhóm nghiệp vụ | Mã danh mục (`TicketCategoryEnum`) | Tên loại thủ tục học vụ | Mức độ ưu tiên | Thời hạn SLA (`slaHours`) | Đơn vị tiếp nhận mặc định |
| :--- | :--- | :--- | :---: | :---: | :--- |
| **Nhóm Khẩn cấp** | `DEFER_EXAM` | Đơn xin hoãn thi / Thi bổ sung | **URGENT** | **24 giờ** | Phòng Đào tạo / Khảo thí |
| | `ACADEMIC_SUSPENSION` | Tạm dừng tiến độ / Bảo lưu học kỳ | **URGENT** | **24 giờ** | Phòng Đào tạo |
| | `DROPOUT_REQUEST` | Đơn xin thôi học / Rút hồ sơ | **URGENT** | **24 giờ** | Phòng Đào tạo |
| | `GRADUATION_APPEAL` | Cứu xét tốt nghiệp / Xét tốt nghiệp bổ sung | **URGENT** | **24 giờ** | Phòng Đào tạo |
| | `EXAM_GRADE_DISPUTE` | Khiếu nại sai sót điểm thi tốt nghiệp | **URGENT** | **24 giờ** | Phòng Khảo thí |
| **Nhóm Ưu tiên cao** | `LMS_PORTAL_LOCK` | Mở khóa tài khoản LMS / Cổng thi trắc nghiệm | **HIGH** | **48 giờ** | TT Công nghệ Thông tin |
| | `COURSE_REGISTRATION_ISSUE` | Xử lý lỗi đăng ký tín chỉ học phần tốt nghiệp | **HIGH** | **48 giờ** | Phòng Đào tạo |
| | `SCHOLARSHIP_DISPUTE` | Thắc mắc / Khiếu nại học bổng khuyến khích | **HIGH** | **48 giờ** | Phòng CTSV |
| **Nhóm Thông thường** | `TUITION_RECONCILIATION` | Đối soát học phí ngân hàng / Trừ tiền trùng | **MEDIUM** | **72 giờ** | Phòng Kế hoạch Tài chính |
| | `TRANSCRIPT_REQUEST` | Trích lục bảng điểm / Xác nhận điểm rèn luyện | **MEDIUM** | **72 giờ** | Phòng Đào tạo / CTSV |
| | `STUDENT_CARD_REISSUE` | Cấp lại thẻ sinh viên / Thẻ ngân hàng liên kết | **MEDIUM** | **72 giờ** | Phòng CTSV |
| | `MILITARY_DEFERMENT` | Cấp giấy hoãn nghĩa vụ quân sự | **MEDIUM** | **72 giờ** | Phòng Đào tạo |
| **Nhóm Tiêu chuẩn** | `PROFILE_UPDATE` | Cập nhật thông tin CCCD / Hộ khẩu / Email | **LOW** | **120 giờ** (5 ngày) | Phòng Đào tạo |
| | `SURVEY_FEEDBACK` | Góp ý khảo sát chất lượng đào tạo | **LOW** | **120 giờ** (5 ngày) | Phòng ĐBCL |
| | `GENERAL_INQUIRY` | Tư vấn hướng nghiệp / Câu hỏi chung | **LOW** | **120 giờ** (5 ngày) | Ban Cố vấn học tập |

### 2.2. Cơ chế Suy diễn SLA Engine (`SlaCalculatorService`)
- Khi người dùng gửi yêu cầu tạo Ticket:
  $$\text{Category} \longrightarrow (\text{Priority}, \text{SLA\_Hours})$$
  $$\text{dueDate} = \text{LocalDateTime.now()} + \text{SLA\_Hours}$$
- Nếu có AI Triage phân tích nội dung khẩn cấp (vd phát hiện tai nạn, nhập viện, vi phạm nghiêm trọng), AI chỉ có quyền nâng hạng (`MEDIUM` $\rightarrow$ `HIGH` hoặc `HIGH` $\rightarrow$ `URGENT`), tuyệt đối không được phép hạ cấp SLA đã cam kết của quy định.

### 2.3. Thiết kế Endpoint & Dynamic Filter Phân trang Backend
- **Endpoint:** `GET /tickets/my-tickets`
- **Request Parameters:**
  - `page`: Chỉ số trang (0-indexed, mặc định: 0)
  - `size`: Số bản ghi mỗi trang (mặc định: 10)
  - `status`: Lọc trạng thái (`ALL`, `OPEN`, `IN_PROGRESS`, `WAITING_FOR_STUDENT`, `RESOLVED`, `CLOSED`)
  - `departmentId`: Lọc theo Khoa/Phòng ban
  - `keyword`: Tìm kiếm từ khóa theo Mã Ticket (`ticketCode`) hoặc Tiêu đề (`title`)
- **Trả về Model:**
  - `tickets`: Đối tượng `Page<TicketResponseDto>` (chứa `content`, `totalPages`, `totalElements`, `number`, `size`, `first`, `last`).
  - `statusCounts`: Map đếm số lượng vé của sinh viên theo từng trạng thái để hiển thị trực tiếp lên Badges của các Tab lọc.
  - `currentStatus`, `selectedDept`, `keyword`: Giữ trạng thái của thanh tìm kiếm.
  - `departments`: Danh sách đơn vị tiếp nhận để hiển thị Dropdown.

### 2.4. Tiêu chuẩn Giao diện Người dùng Mới (`my-tickets.html`)
1. **Loại bỏ 100% Mock Code:** Xóa `#stateSwitcherBar`, xóa toàn bộ TH2 mock data, xóa các block div skeleton/no-search giả lập bằng JS.
2. **Hiển thị Bảng 6 Cột chuẩn:**
   - **Cột 1: Mã Ticket, Tiêu đề & Loại thủ tục:** Mã định dạng Font Mono xanh đậm, Tiêu đề có link xem chi tiết, nhãn loại thủ tục rõ ràng.
   - **Cột 2: Đơn vị tiếp nhận:** Badge Khoa/Phòng tiếp nhận.
   - **Cột 3: Trạng thái tiếp nhận:** Badge màu chuẩn (Chờ tiếp nhận - Slate, Đang xử lý - Blue, Chờ phản hồi - Amber, Đã giải quyết - Emerald, Đã đóng - Gray).
   - **Cột 4: Mức độ ưu tiên:** Badge chỉ đọc (Read-only) do hệ thống gán, có icon chấm tròn trực quan (Khẩn cấp - Rose, Cao - Orange, Trung bình - Amber, Thấp - Slate).
   - **Cột 5: Hạn xử lý (SLA) & Đếm ngược:**
     - Hiển thị ngày tạo và hạn chót `dueDate`.
     - Nhãn trực quan: `Quá hạn SLA` (Đỏ nếu trễ hạn), `Sắp hết hạn` (Vàng nếu còn dưới 12h), `Đúng hạn` (Xanh/Slate).
   - **Cột 6: Thao tác:** Nút icon dẫn trực tiếp tới `/tickets/detail/{id}`.
3. **Phân trang Spring Boot:**
   - Sử dụng thẻ `<a>` với `th:href` chuẩn, bảo toàn toàn bộ URL query param khi bấm chuyển trang.
   - Thanh số trang hỗ trợ Previous / Next, Active trang hiện tại.
4. **Empty State:**
   - Khi `tickets.empty`, hiển thị khối empty chuẩn Bootstrap/Tailwind với icon trang nhã và nút bấm tạo ticket mới.

---

## 3. KẾ HOẠCH BÀN GIAO & QUY TRÌNH THỰC HIỆN
- **Trạng thái hiện tại:** Tạm dừng mọi thao tác code; đã hoàn thành phân tích kỹ thuật và trình bày thiết kế trong `tiendo10.md`.
- **Bước tiếp theo:** Chờ phản hồi và lệnh duyệt ("Duyệt" / "Đồng ý") từ Dev Lead trước khi tiến hành viết code backend và refactor template Thymeleaf.

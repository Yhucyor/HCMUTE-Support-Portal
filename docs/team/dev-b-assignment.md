# 📋 PHÂN CÔNG NHIỆM VỤ: DEV B (FRONTEND LEAD & QA/TESTING SPECIALIST)

* **Họ và tên:** Dev B
* **Vị trí đảm nhận:** Frontend Lead & QA / Testing Specialist
* **Phạm vi trách nhiệm:** Toàn bộ Giao diện người dùng (Thymeleaf UI/UX) & Hệ thống Kiểm thử Toàn diện (Unit Test, Integration Test, Security Test)
* **Tài liệu quy chuẩn tham chiếu:** [engineering-rules.md](file:///d:/HK5/LapTrinhWeb/doancuoiki/HCMUTE-Support-Portal/docs/team/engineering-rules.md)

---

## 🎯 1. TỔNG QUAN PHẠM VI CÔNG VIỆC

Dev B chịu trách nhiệm xây dựng toàn bộ hệ thống giao diện người dùng (Thymeleaf Templates, Stylesheet CSS, Responsive Web Design, Client Javascript, Realtime STOMP WebSockets) đồng thời đóng vai trò Kỹ sư Đảm bảo Chất lượng (QA/QC), xây dựng bộ kiểm thử tự động toàn diện từ tầng Unit Test đến Integration Test và Security Test.

---

## 🎨 2. GIAO DIỆN NGƯỜI DÙNG (FRONTEND & UI/UX ARCHITECTURE)

Dev B xây dựng toàn bộ hệ sinh thái giao diện theo chuẩn thẩm mỹ hiện đại của Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE):

### 2.1. Hệ Thống Layout & Component Tái Sử Dụng (Thymeleaf Dialect)
- `templates/layout/main.html`: Master layout chứa khung cấu trúc chuẩn (HTML5, Meta SEO, Font Inter/Playfair).
- `templates/fragments/navbar.html`: Thanh điều hướng thông minh (hiển thị trạng thái đăng nhập, avatar, menu phân quyền).
- `templates/fragments/sidebar.html`: Thanh công cụ bên hông cho Cán bộ & Quản trị viên (quản lý Ticket, Duyệt bài, Thống kê).
- `templates/fragments/footer.html`: Chân trang thông tin liên hệ các Khoa/Phòng ban và bản quyền HCMUTE.

### 2.2. Chi Tiết Toàn Bộ 18 Trang Giao Diện (Templates)
1. **Cổng Thông Tin Công Khai (Public Portal):**
   - `index.html`: Trang chủ tích hợp banner giới thiệu, lối tắt đến các dịch vụ tư vấn, tin tức nóng và thống kê nhanh.
   - `about.html`: Giới thiệu sứ mệnh hệ thống tư vấn học vụ và thông tin các Khoa/Phòng ban.
   - `admission.html`: Cẩm nang hướng dẫn tuyển sinh, đề án và câu hỏi thường gặp.
   - `research.html`: Hướng dẫn đồ án tốt nghiệp, đề tài nghiên cứu khoa học sinh viên.
   - `explore.html`: Khám phá chi tiết các Khoa và bộ phận hỗ trợ trong trường.

2. **Xác Thực & Quản Lý Hồ Sơ (Authentication & Profile):**
   - `login.html`: Đăng nhập bằng MSSV/Email, ghi nhớ phiên đăng nhập, cảnh báo lỗi thẩm mỹ.
   - `register.html`: Đăng ký tài khoản sinh viên kèm lựa chọn Khoa/Ngành.
   - `profile.html`: Quản lý thông tin cá nhân, cập nhật số điện thoại, đổi mật khẩu và đổi ảnh đại diện.

3. **Hệ Thống Tiếp Nhận & Xử Lý Phiếu Tư Vấn (Ticket Support Desk):**
   - `ticket-list.html`: Danh sách phiếu tư vấn theo tab (Tất cả, Chờ xử lý, Đang xử lý, Đã giải quyết), có bộ lọc Khoa và mức độ ưu tiên.
   - `ticket-detail.html`: Chi tiết phiếu tư vấn, dòng thời gian trao đổi (Message Timeline), khu vực tải tệp đính kèm, nút Đánh giá mức độ hài lòng (1-5 sao) và nút Chuyển khoa/Đóng phiếu.

4. **Diễn Đàn Sinh Viên & Cộng Đồng Khoa (Community & Feed):**
   - `feed.html`: Bảng tin sinh viên dạng thẻ bài viết (Card view), tích hợp thả tim (Like), đếm bình luận và nút báo cáo bài viết.
   - `group.html`: Không gian diễn đàn riêng theo từng Khoa/Phòng ban.
   - `create-post.html`: Soạn thảo bài viết mới với trình đính kèm hình ảnh trực quan.

5. **Trợ Lý Ảo AI & Hỏi Đáp Thông Minh (AI Chatbot & FAQ):**
   - `ai-chat.html`: Giao diện trò chuyện tương tác trực tuyến với Trợ lý ảo AI QAUTE, bong bóng tin nhắn mượt mà, danh sách câu hỏi FAQ gợi ý nhanh và nút bấm "Tạo Ticket ngay" khi AI không giải đáp được.

6. **Bảng Điều Khiển Quản Trị & Thống Kê (Dashboard & Admin Desk):**
   - `dashboard.html`: Biểu đồ tổng quan KPI, tiến độ SLA của từng Khoa, tỷ lệ hài lòng của sinh viên.
   - `error.html`: Trang thông báo lỗi thân thiện (403 Forbidden, 404 Not Found, 500 Error).

### 2.3. Client-Side Javascript & Realtime WebSocket
- Tích hợp thư viện SockJS và STOMP.js để nhận tín hiệu thông báo realtime từ Server.
- Hiệu ứng Badge chuông thông báo đỏ nhấp nháy khi có phản hồi ticket mới.
- Kiểm tra tính hợp lệ dữ liệu biểu mẫu (Client-side validation) mượt mà trước khi submit.

---

## 🧪 3. KIỂM THỬ TOÀN DIỆN (FULL TESTING & QA SUITE)

Dev B xây dựng toàn bộ bộ kiểm thử tự động (Test Automation Suite) cho hệ thống:

### 3.1. Unit Testing (JUnit 5 & Mockito)
- `AuthServiceTest`: Kiểm thử luồng đăng ký tài khoản hợp lệ, trùng username/email, kiểm thử đăng nhập sai mật khẩu, kiểm thử cơ chế OTP.
- `TicketServiceTest`: Kiểm thử luồng tạo Ticket, chuyển trạng thái vòng đời, tính toán hạn SLA 24h/72h.
- `FeedServiceTest`: Kiểm thử tạo bài viết, kiểm tra bộ lọc từ khóa cấm, tăng giảm lượt tương tác.
- `AiRagServiceTest`: Kiểm thử tính điểm Cosine Similarity, kiểm thử điều kiện kích hoạt Fallback Ticket.

### 3.2. Integration Testing (`@SpringBootTest`, `@AutoConfigureMockMvc`)
- `AuthIntegrationTest`: Kiểm thử gọi API `/api/v1/auth/login` và `/api/v1/auth/register` với HTTP status và JSON response chuẩn `ApiResponse<T>`.
- `TicketIntegrationTest`: Kiểm thử toàn bộ chu trình tạo ticket $\rightarrow$ gửi phản hồi $\rightarrow$ đóng ticket.
- `DepartmentIsolationTest`: Kiểm thử bảo mật tầng dữ liệu (Data Isolation): Đảm bảo Cán bộ thuộc Khoa CNTT không thể truy cập API lấy dữ liệu của Phòng Đào tạo.

### 3.3. Security & Access Control Testing
- Kiểm thử các URL yêu cầu phân quyền (`/admin/**`, `/staff/**`, `@PreAuthorize`).
- Đảm bảo người dùng chưa đăng nhập bị chuyển hướng về `/auth/login`.

### 3.4. Tài Liệu Kịch Bản Kiểm Thử & Postman Collection
- Xây dựng bộ kịch bản kiểm thử chi tiết (Test Cases Matrix) bao gồm Test Case ID, Mục tiêu, Dữ liệu đầu vào, Kết quả kỳ vọng và Kết quả thực tế.
- Bộ sưu tập Postman Collection xuất file `.json` chứa đầy đủ các endpoint API kèm biến môi trường (`{{jwt_token}}`, `{{base_url}}`).

---

## 📦 4. SẢN PHẨM BÀN GIAO CỦA DEV B
1. Toàn bộ file giao diện: `src/main/resources/templates/` (18 HTML templates + fragments).
2. Toàn bộ mã nguồn kiểm thử: `src/test/java/com/school/counseling/` (Unit Test, Integration Test, Security Test).
3. Đảm bảo chạy lệnh `mvn test` vượt qua 100% tất cả các test cases (`BUILD SUCCESS`).

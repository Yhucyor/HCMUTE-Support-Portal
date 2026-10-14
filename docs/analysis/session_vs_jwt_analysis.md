# PHÂN TÍCH KIẾN TRÚC XÁC THỰC: TẠI SAO NÊN DÙNG SESSION COOKIE THAY VÌ JWT CHO DỰ ÁN QAUTE PORTAL

---

## 1. TỔNG QUAN KIẾN TRÚC DỰ ÁN
Dự án **QAUTE Portal** được định hình theo mô hình:
- **Kiến trúc:** Monolithic Web Application (Server-Side Rendering - SSR).
- **Backend & View:** Java Spring Boot 3 + Thymeleaf + Bootstrap 5.
- **Tương tác:** Form Submit, Chuyển hướng trang (Redirects với Flash Messages), kết hợp một phần nhỏ WebSocket STOMP (Chat/Thông báo).

---

## 2. SO SÁNH CHI TIẾT: SESSION COOKIE VS JWT COOKIE

| Tiêu chí | Session Cookie (`JSESSIONID`) | JWT Cookie (`access_token`) | Đánh giá với đồ án này |
| :--- | :--- | :--- | :--- |
| **Mô hình phù hợp** | Monolith, Server-Side Rendering (Thymeleaf, JSP) | SPA (React, Vue, Angular), Microservices, Mobile App | **Session thắng tuyệt đối** |
| **Tích hợp Spring Security** | Mặc định, tự nhiên, chỉ cần 5 dòng config | Phải tự viết `Filter`, `Provider`, giải mã, bắt exception | Session ít lỗi, code sạch |
| **Hủy phiên tức thì (Revocation)** | Server chỉ cần xóa Session (khi Logout, Khóa tài khoản, Đổi khoa) | Không thu hồi được trừ khi hết hạn hoặc dùng Redis Blacklist | Session bảo mật hơn cho nghiệp vụ trường học |
| **Quản lý Flash Messages** | Hỗ trợ tự nhiên qua `RedirectAttributes` sau khi POST form | Khó quản lý Flash Message khi chuyển trang | Session mượt mà hơn |
| **Bảo mật CSRF / XSS** | Cookie `HttpOnly`, `SameSite=Lax`, bật sẵn Spring CSRF | Vẫn cần phòng chống CSRF nếu để trong Cookie | Session chuẩn chỉ hơn |
| **Kích thước gói tin (Overhead)** | Nhẹ (~32 bytes chuỗi Session ID) | Nặng (1KB – 2KB vì chứa Header, Payload, Signature) | Session nhẹ hơn |

---

## 3. CÁC LÝ DO CỐT LÕI NÊN DÙNG SESSION COOKIE CHO DỰ ÁN NÀY

### 3.1. Thu hồi quyền và Khóa tài khoản tức thì (Instant Invalidation)
- **Đặc thù nghiệp vụ:** Hệ thống có phân quyền Cán bộ theo Khoa/Phòng (`ROLE_STAFF`). Nếu một Cán bộ bị chuyển công tác, khóa tài khoản hoặc hạ quyền, Admin thực hiện thao tác trên web:
  - **Với Session:** Server chỉ cần `session.invalidate()`, tài khoản bị logout ngay ở request tiếp theo.
  - **Với JWT:** Token vẫn còn hạn (ví dụ 1 tiếng), Cán bộ đó vẫn có thể dùng token cũ để xem thông tin Ticket nhạy cảm của Khoa. Nếu muốn chặn ngay, bạn phải dựng thêm **Redis** để lưu danh sách đen Token (Token Blacklist), biến hệ thống thành stateful và làm phức tạp hóa đồ án không cần thiết.

### 3.2. Tương thích hoàn hảo với Thymeleaf & Spring Security 6
- Trong các trang Thymeleaf, bạn có thể dễ dàng kiểm tra quyền:
  ```html
  <div sec:authorize="hasRole('STAFF')">...</div>
  <span sec:authentication="name"></span>
  ```
  Spring Security tự động liên kết `SecurityContext` từ Session vào luồng render HTML của Thymeleaf mà không cần qua bất kỳ tầng trung gian giải mã token nào.

### 3.3. Hỗ trợ luồng Post-Redirect-Get (PRG) và Flash Messages
- Khi Sinh viên gửi Ticket hoặc Cán bộ đăng bài thông báo, chuẩn thiết kế web là:
  `POST /ticket/create` $\rightarrow$ xử lý $\rightarrow$ `Redirect: /ticket/list` kèm thông báo *"Tạo ticket thành công"*.
- `RedirectAttributes.addFlashAttribute("success", "...")` của Spring MVC hoạt động dựa vào Session để lưu tạm thông báo trong 1 lần redirect duy nhất. Dùng JWT sẽ khiến việc truyền flash messages giữa các lần redirect trở nên phức tạp.

### 3.4. Bảo vệ dữ liệu & Phòng chống tấn công (Security)
- Cookie Session được cấu hình `HttpOnly; SameSite=Lax; Secure`, JavaScript ở trình duyệt hoàn toàn **không thể đọc được**, ngăn chặn 100% nguy cơ bị đánh cắp qua tấn công XSS.
- Spring Security 6 tự động kích hoạt `CsrfTokenRepository` gắn vào tất cả các form `<form th:action="..." method="post">` của Thymeleaf, bảo vệ toàn diện hệ thống.

---

## 4. KẾT LUẬN & KIẾN NGHỊ

1. **Khẳng định:** Với đồ án Spring Boot 3 + Thymeleaf, **Session Cookie là sự lựa chọn chuẩn kỹ thuật, tối ưu và sạch nhất**.
2. **Kế hoạch cho WebSocket:** Khi kết nối WebSocket STOMP (`/ws-chat`), Spring WebSocket có thể dùng trực tiếp Session của HTTP Handshake mà không cần phải truyền thêm JWT token phức tạp.
3. **Mở rộng tương lai (Nếu có):** Chỉ bổ sung JWT Filter nếu sau này nhóm quyết định viết thêm ứng dụng Mobile (Flutter/Android) hoặc tách riêng giao diện ReactJS.

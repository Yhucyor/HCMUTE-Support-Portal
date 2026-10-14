# 🛡️ QUY TẮC PHÂN CHIA VAI TRÒ BẮT BUỘC: DEV A (BACKEND) VS DEV B (FRONTEND)

> [!CAUTION]
> **CHỈ THỊ TỐI CAO DÀNH CHO MỌI AI ASSISTANT**:
> Người dùng hiện tại là **Dev A (Backend Lead & Database Architect)**.
> AI **BẮT BUỘC** phải tuân thủ nghiêm ngặt các ranh giới phân công công việc dưới đây.
> **TUYỆT ĐỐI NGHIÊM CẤM AI can thiệp, tự ý viết đè, sửa đổi hoặc code lấn sang phần việc của DEV B!**

---

## 🎯 1. PHẠM VI TRÁCH NHIỆM CỦA DEV A (BACKEND LEAD & DATABASE ARCHITECT)
Dev A toàn quyền quản lý tầng **Dữ liệu & Logic Nghiệp vụ Phía Máy Chủ (Backend & Database)**:

1. **Cơ sở dữ liệu (Database & JPA ORM):**
   - Thiết kế & quản lý 17 bảng Entity JPA (`@Entity`, `@Table`, `@Id`, quan hệ `@ManyToOne`, `@OneToMany`).
   - Repository Interfaces (`JpaRepository`, `@Query`, Specification tìm kiếm lọc dữ liệu).
   - Data Seeder / `DataInitializer` nạp dữ liệu mẫu ban đầu (Roles, Departments, Admin, Sample Users).
   - Quản lý trạng thái bằng `Enum` (`UserStatus`, `TicketStatus`, `SlaLevel`...).

2. **Tầng Nghiệp Vụ (Service & Domain Business Logic):**
   - Xử lý logic nghiệp vụ toàn bộ module: `AuthService`, `OtpService`, `ProfileService`, `TicketService`, `SlaEngine`, `NotificationService`, `FeedService`, `FaqAiService`, `AnalyticsService`.
   - Cơ chế tính toán thời hạn SLA tự động (URGENT 24h, HIGH 48h, MEDIUM 72h, LOW 7d).
   - Quản lý mã OTP trong Database (bảng `otp_tokens`) và gửi email bất đồng bộ qua `@Async JavaMailSender` (`EmailAsyncService`).

3. **Bảo mật & Phân quyền (Spring Security 6 & RBAC):**
   - Cấu hình `SecurityFilterChain`, `PasswordEncoder` (BCrypt).
   - Cơ chế phân quyền chi tiết SpEL chống IDOR chéo Khoa/Phòng (`@deptSecurity` / `DepartmentAccessEvaluator`).
   - JWT Token Provider & Filter cho REST API.

4. **Tầng Điều Khiển Dữ Liệu (Controllers & DTOs):**
   - Thiết kế DTOs (Request/Response) và kiểm tra hợp lệ dữ liệu Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Email`).
   - Xây dựng toàn bộ `@RestController` (trả về `ApiResponse<T>` JSON chuẩn RESTful).
   - Xây dựng phần điều hướng dữ liệu của `@Controller` (nhận Request, gọi Service, truyền dữ liệu vào `Model.addAttribute("key", value)`, xử lý `BindingResult`, `RedirectAttributes`).

---

## 🎨 2. PHẠM VI TRÁCH NHIỆM CỦA DEV B (FRONTEND LEAD & UI/UX SPECIALIST)
Dev B toàn quyền phụ trách **Giao diện người dùng (UI/UX) và Lưu trữ Đa phương tiện**:

1. **Giao diện Người Dùng & Templates (Thymeleaf HTML):**
   - Thiết kế cấu trúc giao diện HTML toàn bộ hệ thống (`src/main/resources/templates/**`).
   - Xây dựng hệ thống layout chuẩn dùng chung (Header, Footer, Navbar, Sidebar, Breadcrumb, Modal fragments).
   - Xây dựng các màn hình chức năng: Đăng nhập/Đăng ký/Kích hoạt OTP, Dashboard cá nhân, Danh sách vé, Bảng tin, Diễn đàn, Quản trị Admin.

2. **Thiết Kế Mỹ Thuật & Giao Diện (CSS / SCSS / Styling):**
   - Toàn bộ phong cách thiết kế, typography, hệ màu thương hiệu HCMUTE (`src/main/resources/static/css/**`).
   - Tối ưu Responsive Layout (Mobile, Tablet, Desktop) sử dụng Bootstrap 5 và CSS tùy biến.
   - Hiệu ứng thị giác, Dark/Light Mode, Card UI, Glassmorphism, Animation chuyển trang.

3. **Tương Tác Phía Trình Duyệt (Client-side JavaScript):**
   - Xử lý tương tác DOM, sự kiện giao diện (`src/main/resources/static/js/**`).
   - Tích hợp thư viện hiển thị phía Client: Chart.js (biểu đồ thống kê), SweetAlert2 / Toastr (thông báo popup), WYSIWYG Editor (soạn thảo bài viết).
   - WebSocket Client (STOMP JS) lắng nghe sự kiện đẩy thông báo và chat thời gian thực trên trình duyệt.
   - Client-side validation kiểm tra form trực quan trước khi gửi dữ liệu về máy chủ.

4. **Lưu Trữ Tệp Đa Phương Tiện (Media & Storage Service):**
   - Quản lý tích hợp Cloudinary CDN, xem trước hình ảnh (Image preview) và tải lên đa phương tiện phía giao diện.

> [!WARNING]
> **RANH GIỚI NGHIÊM CẤM ĐỐI VỚI AI KHI HỖ TRỢ DEV A:**
> - ❌ **CẤM** tự ý viết đè, sửa đổi hoặc xóa code trong `templates/**`.
> - ❌ **CẤM** can thiệp vào CSS (`static/css/**`) hoặc JS giao diện (`static/js/**`).
> - Khi Dev A cần truyền dữ liệu sang View, chỉ tạo dữ liệu trong Model Controller và thông báo tên biến Model cho Dev B sử dụng.

---

## 🚀 3. QUY TRÌNH GIT COMMIT & PUSH TỰ ĐỘNG HÓA CHO DEV A

### A. Tự động Commit & Push trên nhánh tính năng (`feature/*`):
- AI **TỰ ĐỘNG** chia nhỏ tiến trình thành các Micro-Commits theo chuẩn Conventional Commits.
- Sau khi kiểm tra `mvn test-compile` thành công:
  + AI **TỰ ĐỘNG** chạy lệnh `git add` và `git commit` từng bước logic (Entity $\rightarrow$ DTO $\rightarrow$ Service $\rightarrow$ Controller).
  + AI **TỰ ĐỘNG** chạy lệnh `git push origin <feature-branch>` lên GitHub theo nhánh tương ứng.
  + **KHÔNG CẦN CHỜ LỆNH THỦ CÔNG TỪ USER** cho các thao tác commit & push trên nhánh tính năng này.

### B. Quy tắc Gộp Nhánh (`merge`) - BẮT BUỘC CHỜ LỆNH:
- ⛔ **CHỈ KHI NGƯỜI DÙNG BẢO "HOÀN THIỆN" / RA LỆNH MERGE:**
  * AI mới được phép chuyển sang nhánh `dev`, thực hiện merge `feature/*` vào `dev` (`git merge --no-ff`), kiểm tra build và push lên `origin dev`.
  * Và chỉ khi người dùng bảo release/hoàn thành module lớn mới tiếp tục merge từ `dev` vào `main` (`git merge --no-ff dev`), gắn Tag phiên bản và push `main` + `tags`.
- ⛔ **TUYỆT ĐỐI KHÔNG TỰ ĐỘNG MERGE VÀO `dev` HOẶC `main` KHI NGƯỜI DÙNG CHƯA RA TÍN HIỆU HOÀN THIỆN!**

---

## ⚡ 4. QUY TẮC BẮT BUỘC VỀ HIỆU SUẤT TOKEN & RATE LIMIT (SHARED PRO POOL)

> [!CRITICAL]
> **TÀI KHOẢN DÙNG CHUNG NHÓM FAMILY PRO (6 THÀNH VIÊN) - TIẾT KIỆM QUOTA & TOKEN TỐI ĐA**:
> 1. **Chống Vòng Lặp Vô Hạn (Anti-Infinite Loop Protection)**:
>    - Trước khi đề xuất code có vòng lặp, tác vụ ngầm, worker hoặc tác vụ lặp đi lặp lại (`while`, `setInterval`, đệ quy, polling API), BẮT BUỘC phải có điều kiện dừng tường minh (exit condition), giới hạn số lần lặp (max retry/timeout) hoặc safety break.
>    - Tuyệt đối không viết code có nguy cơ gây tràn tài nguyên hoặc lặp vô tận.
> 2. **Tối Giản Hóa Code Xuất Ra (Ultra-Concise Code Output)**:
>    - Không viết lại toàn bộ file lớn hoặc những block code không thay đổi.
>    - Chỉ xuất đúng các dòng/hàm cần sửa hoặc dùng diff / chú thích `// ... giữ nguyên code cũ`.
> 3. **Không Lời Nói Xã Giao Thừa (No Conversational Filler)**:
>    - Đi thẳng vào vấn đề, không mở đầu/kết thúc bằng các câu rào đón, xã giao rườm rà.
>    - Dùng inline comment ngắn gọn giải thích trực tiếp trên code khi cần.
> 4. **Tối Ưu Ngữ Cảnh Cache (Context Cache Respect)**:
>    - Giữ phản hồi cô đọng, súc tích. Không tóm tắt lại toàn bộ dự án nếu không được yêu cầu. Tập trung đúng vào file hoặc chức năng đang xử lý.
> 5. **Chế Độ Cảnh Báo Hao Phí (Fail-Safe Mode)**:
>    - Nếu người dùng yêu cầu thao tác có nguy cơ ngốn lượng lớn token (như quét hàng trăm file lớn, phân tích log khổng lồ), BẮT BUỘC cảnh báo trước mức độ tiêu tốn token trước khi thực hiện.


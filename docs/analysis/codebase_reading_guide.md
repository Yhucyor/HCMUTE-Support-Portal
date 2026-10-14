# 📖 HƯỚNG DẪN ĐỌC CODE & GIẢI THÍCH KIẾN TRÚC CẤU TRÚC THƯ MỤC DỰ ÁN QAUTE PORTAL

- **Mã tài liệu:** `docs/analysis/codebase_reading_guide.md`
- **Dự án:** QAUTE Portal - HCMUTE Student Support & Counseling Portal
- **Đối tượng:** Lập trình viên mới tiếp nhận dự án, Thành viên đội ngũ phát triển, Hội đồng đánh giá đồ án
- **Kiến trúc áp dụng:** **Modular Monolith** kết hợp **Domain-Driven Design (DDD Lite)** trên nền tảng **Spring Boot 3.3.x & Java 17**

---

## 🗺️ 1. TỔNG QUAN BẢN ĐỒ CẤU TRÚC THƯ MỤC (PROJECT DIRECTORY TREE)

```text
doancuoiki_demo1/
├── src/                               # Toàn bộ mã nguồn chính của ứng dụng
│   ├── main/
│   │   ├── java/com/school/counseling/
│   │   │   ├── QautePortalApplication.java   # [ĐIỂM XUẤT PHÁT] Hàm main khởi động Spring Boot
│   │   │   ├── common/                       # Các thành phần dùng chung toàn hệ thống
│   │   │   ├── config/                       # Các lớp cấu hình Bean hệ thống (Security, Async, WebMvc...)
│   │   │   └── module/                       # Các module nghiệp vụ lõi (Modular Monolith)
│   │   └── resources/
│   │       ├── application.yml               # File cấu hình trung tâm (Database, Port, API Key, Mail...)
│   │       └── templates/                    # Giao diện Thymeleaf Server-Side Rendering (HTML5 + Bootstrap 5)
│   └── test/                                 # Unit Test & Integration Test (JUnit 5, Mockito)
├── tests/
│   └── e2e/                           # Bộ kiểm thử tự động End-to-End toàn diện bằng Playwright
├── video-generator/                   # Microservice độc lập (Node.js) tự động dựng video thông báo
└── docs/                              # Tài liệu kỹ thuật, đặc tả nghiệp vụ, tiến độ và phân tích
    ├── analysis/                      # Các bài phân tích chuyên sâu (JWT vs Session, Kiến trúc code...)
    ├── dataset/                       # Dữ liệu 2.600+ câu hỏi đáp học vụ thực tế phục vụ AI RAG
    ├── progress/                      # Báo cáo tiến độ & thiết kế kiến trúc (tiendo.md, tiendo2.md, tiendo3.md)
    ├── prompts/                       # Chuỗi Prompt Chain phục vụ sinh code và phát triển AI
    ├── requirements/                  # Đặc tả yêu cầu phần mềm (SRS, brief) & Script SQL (schema.sql)
    └── team/                          # Phân công nhiệm vụ thành viên
```

---

## 🔍 2. GIẢI THÍCH CHI TIẾT TỪNG THƯ MỤC TRONG MÃ NGUỒN JAVA

Toàn bộ backend được viết trong package `com.school.counseling`. Dưới đây là chức năng chi tiết của từng gói:

### 2.1. File Khởi Điểm Hệ Thống
* 🚀 **[QautePortalApplication.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/QautePortalApplication.java)**:
  * Điểm nhập (`main`) nạp toàn bộ Spring ApplicationContext.
  * Kích hoạt quét Bean, tự động cấu hình (Auto-configuration), kích hoạt Scheduler (`@EnableScheduling`) và Async (`@EnableAsync`).

---

### 2.2. Gói `common` - Thành Phần Nền Tảng Dùng Chung
Chứa các thành phần cốt lõi mà mọi module khác đều kế thừa hoặc sử dụng:

| Thư mục con | Các File Tiêu Biểu | Ý Nghĩa Chức Năng |
| :--- | :--- | :--- |
| `common/entity/` | [BaseEntity.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/common/entity/BaseEntity.java) | Lớp cha (`@MappedSuperclass`) cho tất cả Entity trong CSDL. Quản lý tự động các trường: `created_at`, `updated_at`, `created_by` và cờ xóa mềm `is_deleted`. |
| `common/dto/` | `ApiResponse.java`, `PageResponse.java` | Định dạng chuẩn gói tin trả về từ Controller (Status, Message, Data, Timestamp), tạo sự đồng nhất 100% cho RESTful API. |
| `common/exception/` | `GlobalExceptionHandler.java`, `ResourceNotFoundException.java` | Bắt lỗi tập trung trên toàn ứng dụng bằng `@RestControllerAdvice` và `@ControllerAdvice`. Chuyển đổi mã lỗi hệ thống thành thông báo thân thiện. |
| `common/storage/` | `StorageService.java`, `LocalStorageService.java`, `CloudinaryService.java` | Module trừu tượng hóa việc lưu trữ file (Dual-mode: Chạy cục bộ vào `C:\upload` hoặc đẩy lên Cloudinary CDN). |
| `common/util/` | `Constant.java`, `DateTimeUtils.java` | Định nghĩa các hằng số hệ thống, định dạng ngày giờ và tiện ích bổ trợ. |

---

### 2.3. Gói `config` - Cấu Hình Hạ Tầng Hệ Thống
Chứa các lớp cấu hình kỹ thuật cấp thấp:

* 🛡️ **`SecurityConfig.java`**: 
  * Cấu hình **Spring Security 6**.
  * Định nghĩa chuỗi lọc `SecurityFilterChain`: Thiết lập cookie `JSESSIONID` an toàn (`HttpOnly`, `SameSite=Lax`, CSRF).
  * Phân quyền truy cập các URL: `/admin/**` (Chỉ Admin), `/staff/**` (Cán bộ Khoa/Phòng), `/student/**` (Sinh viên), `/public/**` (Tự do).
* ⚡ **[AsyncConfig.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/config/AsyncConfig.java)**:
  * Khởi tạo `ThreadPoolTaskExecutor` phục vụ các tác vụ chạy ngầm bất đồng bộ (gửi email thông báo, nạp dữ liệu vector AI) mà không chặn luồng chính của người dùng.
* 🌐 **`WebMvcConfig.java`**:
  * Cấu hình Resource Handlers (ánh xạ thư mục lưu file upload ra URL truy cập trên trình duyệt), cấu hình CORS và định dạng dữ liệu trả về.

---

### 2.4. Gói `module` - Các Phân Hệ Nghiệp Vụ (Modular Monolith)
Dự án được chia tách thành các phân hệ độc lập về nghiệp vụ. Mỗi module tuân thủ cấu trúc 4 tầng: **Controller $\rightarrow$ Service $\rightarrow$ Repository $\rightarrow$ Entity**:

#### 1. `module/auth` (Xác thực & Quản lý Người dùng / Đơn vị)
* **Chức năng:** Đăng nhập, đăng ký, quên mật khẩu, xác thực mã OTP qua Email, phân quyền RBAC (`ROLE_STUDENT`, `ROLE_STAFF`, `ROLE_ADMIN`).
* **Entity cốt lõi:**
  * `User.java`: Thông tin tài khoản, mật khẩu mã hóa BCrypt, trạng thái kích hoạt.
  * `Role.java`: Quyền hạn hệ thống.
  * `Department.java`: Các đơn vị (Khoa CNTT, Phòng Đào tạo, Phòng Tuyển sinh...).
  * `OtpToken.java`: Quản lý mã OTP gửi qua Email có thời hạn 5 phút.

#### 2. `module/ticket` (Quản Lý Yêu Cầu Hỗ Trợ & Động Cơ SLA Deadline)
* **Chức năng:** Trọng tâm đồ án. Quản lý toàn bộ vòng đời ticket khi sinh viên gửi câu hỏi cần cán bộ can thiệp.
* **Động cơ SLA (Service Level Agreement):** Tự động tính hạn chót xử lý (`due_date`) dựa theo mức ưu tiên:
  * `URGENT` $\rightarrow$ Hạn chót: 24 giờ.
  * `MEDIUM` $\rightarrow$ Hạn chót: 72 giờ (3 ngày).
  * `LOW` $\rightarrow$ Hạn chót: 7 ngày.
* **Cơ chế Phân quyền Khoa:** Cán bộ Khoa A không được xem/sửa Ticket của Khoa B.
* **Entity cốt lõi:** `Ticket.java`, `TicketMessage.java`, `TicketHistory.java`.

#### 3. `module/feed` (Bảng Tin Đa Phương Tiện & Diễn Đàn Sinh Viên)
* **Chức năng:** 
  * *Bảng tin chính thức:* Cán bộ đăng tải công văn, quy chế đính kèm file văn bản và Video MP4.
  * *Diễn đàn cộng đồng:* Sinh viên tạo bài viết thảo luận. Bài viết phải qua kiểm duyệt (`PENDING_APPROVAL` $\rightarrow$ `APPROVED`).
  * *Tương tác & Báo cáo:* Thả tim (Reaction), bình luận (Comment), trung tâm báo cáo bài viết vi phạm (Report).
* **Entity cốt lõi:** `Post.java`, `PostAttachment.java`, `PostComment.java`, `PostReport.java`.

#### 4. `module/chat` (Hội Thoại Trực Tuyến & WebSocket STOMP)
* **Chức năng:** Hỗ trợ sinh viên chat trực tiếp với Cán bộ tư vấn viên trong giờ hành chính theo giao thức WebSocket hai chiều (`/ws-chat`).
* **Tính năng đặc biệt:** Hỗ trợ nút *"Chuyển thành Ticket chính thức"* khi vấn đề phức tạp cần giải quyết qua văn bản.

#### 5. `module/ai` (Trợ Lý AI RAG & Tìm Kiếm Phân Tầng Tri Thức)
* **Chức năng:** Trợ lý ảo AI trả lời tự động 24/7 dựa trên văn bản quy chế và 2.600+ câu hỏi đáp học vụ thực tế.
* **Các thành phần cốt lõi:**
  * [RagChatbotService.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/ai/service/RagChatbotService.java): Điều phối luồng hỏi đáp, kiểm tra Response Cache, gắn Guardrails chống ảo giác (Hallucination).
  * [RagKnowledgeService.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/ai/service/RagKnowledgeService.java): Triển khai thuật toán **Tìm kiếm Phân tầng (Hierarchical Retrieval)**: Quét Tầng 1 (Quy chế chính thức) trước, nếu không có mới tìm Tầng 2 (Lịch sử tư vấn cũ có cảnh báo).
  * [GeminiApiClient.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/ai/service/GeminiApiClient.java): Giao tiếp với Google Gemini API (`text-embedding-004` để tạo vector 768 chiều và `gemini-1.5-flash` để tổng hợp phản hồi).
  * [AcademicAbbreviationUtils.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/module/ai/service/AcademicAbbreviationUtils.java): Chuẩn hóa từ viết tắt học vụ phổ biến của HCMUTE (vd: `avđr` $\rightarrow$ `anh văn đầu ra`, `đrl` $\rightarrow$ `điểm rèn luyện`).

#### 6. `module/integration` (Tích Hợp Webhook Video Tự Động)
* **Chức năng:** Nhận webhook an toàn (bảo vệ bằng chữ ký số HMAC-SHA256) từ microservice `video-generator` khi quá trình render video hoàn tất để cập nhật link video vào bài viết Bảng tin.

---

## 🎨 3. CẤU TRÚC GIAO DIỆN (FRONTEND THYMELEAF)

Nằm trong `src/main/resources/templates/`. Giao diện được thiết kế theo phong cách *Academic Minimalist* sử dụng **Bootstrap 5** và **Thymeleaf Layout Dialect**:

```text
templates/
├── layout/
│   ├── base.html              # Khung giao diện chuẩn (Navbar, Footer, Import CSS/JS)
│   ├── navbar.html            # Thanh điều hướng tự động đổi menu theo Role đăng nhập
│   └── footer.html            # Chân trang thông tin liên hệ HCMUTE
├── auth/                      # Màn hình Đăng nhập, Đăng ký, Quên mật khẩu, Nhập OTP
├── ticket/                    # Màn hình Quản lý Ticket (Danh sách phân màu SLA, Chi tiết, Tạo mới)
├── feed/                      # Giao diện Bảng tin tin tức và Feed diễn đàn thảo luận
├── ai/                        # Giao diện Khung chat Trợ lý AI RAG tương tác thông minh
├── moderation/                # Dashboard dành cho Cán bộ/Admin duyệt bài viết và xử lý tố cáo
└── error/                     # Trang thông báo lỗi thân thiện (403 Forbidden, 404 Not Found, 500 Error)
```

---

## 🧪 4. THƯ MỤC KIỂM THỬ TỰ ĐỘNG (TESTING SUITE)

### 4.1. Unit Test & Integration Test (`src/test/java/`)
* Viết bằng **JUnit 5** và **Mockito**.
* Kiểm thử độc lập logic nghiệp vụ các Service (ví dụ: [SmartFaqMatcherServiceTest.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/test/java/com/school/counseling/module/ai/service/SmartFaqMatcherServiceTest.java)).
* Đảm bảo tính toán đúng Cosine Similarity, quy đổi từ viết tắt và tính chính xác Deadline SLA.

### 4.2. End-to-End Automation Testing (`tests/e2e/`)
* Sử dụng framework **Playwright (TypeScript)** áp dụng mô hình **Page Object Model (POM)**.
* Kiểm thử trọn vẹn trải nghiệm người dùng thực tế trên trình duyệt:
  * `scenario1_auth_flow.spec.ts`: Đăng nhập, phân quyền, kiểm tra session.
  * `scenario2_post_lifecycle_moderation.spec.ts`: Sinh viên đăng bài $\rightarrow$ Cán bộ duyệt $\rightarrow$ Hiển thị trên bảng tin.
  * `scenario3_multicontext_ticket_sla.spec.ts`: Tạo ticket $\rightarrow$ Phân luồng về Khoa $\rightarrow$ Cán bộ nhận và trả lời $\rightarrow$ Tính toán cảnh báo SLA.

---

## 🧭 5. HƯỚNG DẪN LUỒNG ĐỌC CODE HIỆU QUẢ NHẤT (CODE READING FLOW)

Nếu bạn là người mới tiếp cận codebase này, hãy đọc theo trình tự 4 bước logic sau:

```
[1. Bắt đầu từ CSDL & Cấu hình]
   └── Đọc docs/requirements/database/schema.sql & src/main/resources/application.yml
           │
           ▼
[2. Đọc Mô Hình Dữ Liệu Entity]
   └── Đọc BaseEntity.java -> User.java -> Department.java -> Ticket.java -> KnowledgeChunk.java
           │
           ▼
[3. Đọc Bảo Mật & Luồng Nghiệp Vụ Chính]
   └── Đọc SecurityConfig.java -> TicketService.java -> RagChatbotService.java
           │
           ▼
[4. Đọc Tầng Giao Tiếp & Giao Diện]
   └── Đọc Controller tương ứng -> templates HTML Thymeleaf -> Playwright Specs
```

1. **Bước 1: Nắm bắt kiến trúc dữ liệu:** Mở file [schema.sql](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/docs/requirements/database/schema.sql) và [application.yml](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/resources/application.yml) để hiểu cách hệ thống kết nối DB, phân chia các bảng và cấu hình bảo mật.
2. **Bước 2: Xem các Entity đại diện:** Mở [BaseEntity.java](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/src/main/java/com/school/counseling/common/entity/BaseEntity.java) để hiểu cơ chế Soft-delete và Audit, sau đó xem `Ticket.java`, `User.java`, `Post.java`.
3. **Bước 3: Hiểu luồng xử lý nghiệp vụ tại Service:**
   * Xem `TicketService.java` để xem logic chuyển đổi tin nhắn sang Ticket và thuật toán cộng giờ hạn chót SLA.
   * Xem `RagKnowledgeService.java` để xem cách thức nạp vector vào RAM và thuật toán tìm kiếm phân tầng.
4. **Bước 4: Kiểm tra tầng kiểm thử:** Chạy lệnh `npx playwright test` trong thư mục `tests/e2e/` để xem kịch bản người dùng chạy trực quan trên màn hình.

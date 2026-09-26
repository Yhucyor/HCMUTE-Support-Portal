# DANH SÁCH NHIỆM VỤ & TIÊU CHÍ NGHIỆM THU: DEV B

* **Kỹ sư đảm nhiệm:** Dev B (Frontend/Fullstack, Feed, Moderation & Webhook)
* **Quy chuẩn thực thi:** [docs/brief.md](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/docs/brief.md), [docs/team/engineering-rules.md](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/docs/team/engineering-rules.md)

---

## 🟢 SPRINT 1: LAYOUT HỌC VIỆN TỐI GIẢN & SERVICE LƯU TRỮ DUAL STORAGE

- [ ] **`[DEV-B-SP1-01]` Xây Dựng Khung Giao Diện Bootstrap 5 Chuẩn Tối Giản (Academic Minimalist)**
  * **File:** `templates/layout/main.html`, `templates/layout/navbar.html`, `templates/layout/footer.html`
  * **Ràng buộc nghiệp vụ:** Tiêu chuẩn UI/UX trong `engineering-rules.md`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* người dùng truy cập bất kỳ trang nào trên hệ thống.
    * *When* trang tải xong.
    * *Then* giao diện sử dụng tông màu nền xám sáng `#f8f9fa`, Navbar màu xanh Navy `#1e3a8a`, không có màu mè chói lóa hay gradient sặc sỡ, responsive mượt trên Mobile và Desktop.
  * **Manual Test Procedure:**
    * *Kiểm tra:* Mở trình duyệt, đổi kích thước cửa sổ từ 375px (Mobile) đến 1920px (Desktop).
    * *Kỳ vọng UI:* Navbar co giãn chuẩn (Hamburger menu trên Mobile), Card nội dung sắc nét, chữ rõ ràng.

- [ ] **`[DEV-B-SP1-02]` Triển Khai IStorageService (Cloudinary + Local Storage Dự Phòng)**
  * **File:** `common/storage/IStorageService.java`, `common/storage/CloudinaryStorageServiceImpl.java`, `common/storage/LocalStorageServiceImpl.java`, `config/StorageConfig.java`
  * **Ràng buộc nghiệp vụ:** Mục 5 trong `engineering-rules.md`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* một tệp tin tài liệu (`.pdf`, `.docx`) hoặc video (`.mp4`) tải lên.
    * *When* gọi `storageService.uploadFile(multipartFile, folder)`.
    * *Then* kiểm tra Magic Bytes đúng chuẩn định dạng, lưu vào Cloudinary (hoặc thư mục cục bộ `uploads/`) và trả về `StorageResult` chứa URL truy cập công khai.
  * **Manual Test Procedure:**
    * *Test case giả mạo:* Đổi tên file `.exe` thành `tailieu.pdf` rồi upload.
    * *Kỳ vọng:* Service phát hiện sai Magic Byte và từ chối upload với thông báo lỗi.

---

## 🟢 SPRINT 3: BẢNG TIN CHÍNH THỨC, DIỄN ĐÀN, DUYỆT BÀI & WEBHOOK VIDEO

- [ ] **`[DEV-B-SP3-01]` Bảng Tin Chính Thức Đính Kèm File PDF, Word, Excel, Video MP4**
  * **File:** `module/feed/controller/OfficialFeedController.java`, `module/feed/service/PostService.java`, `templates/feed/official-list.html`, `templates/feed/create-official-post.html`
  * **Ràng buộc nghiệp vụ:** `BRULE-POST-001`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* Cán bộ Phòng Tuyển sinh đăng thông báo "Đề án Tuyển sinh 2026".
    * *When* đính kèm file PDF 50MB và bấm "Đăng bài".
    * *Then* bài viết được tạo với `post_type = 'OFFICIAL_ANNOUNCEMENT'`, `status = 'APPROVED'`, hiển thị ngay trên Bảng tin kèm nút Tải file tài liệu.
  * **Manual Test Procedure:**
    * *Input:* Đăng nhập Staff Tuyển sinh, đính kèm file `dean2026.pdf`.
    * *Kỳ vọng UI:* Bài viết xuất hiện trên trang chủ Bảng tin kèm icon PDF và link tải trực tiếp.
    * *Kỳ vọng DB:* Bảng `posts` có `status = 'APPROVED'`; bảng `post_attachments` lưu `file_type = 'PDF'`, dung lượng đúng bytes.

- [ ] **`[DEV-B-SP3-02]` Diễn Đàn Sinh Viên (Feed) & Luồng Kiểm Duyệt Bài Viết**
  * **File:** `module/feed/controller/ForumController.java`, `module/feed/controller/ModerationController.java`, `templates/feed/forum.html`, `templates/moderation/pending-posts.html`
  * **Ràng buộc nghiệp vụ:** `BRULE-POST-002`, `BRULE-POST-003`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* Sinh viên đăng bài thảo luận tìm bạn học nhóm.
    * *When* bài vừa gửi xong.
    * *Then* bài viết ở trạng thái `PENDING_APPROVAL` (chưa hiển thị trên diễn đàn chung), xuất hiện trong Hàng đợi duyệt bài của Cán bộ/Admin. Sau khi Cán bộ bấm "Phê duyệt", bài mới hiển thị công khai.
  * **Manual Test Procedure:**
    * *Bước 1:* Dùng tài khoản Sinh viên đăng bài "Tìm bạn học ôn thi Toeic".
    * *Kỳ vọng:* Trang cá nhân hiện "Đang chờ duyệt". Vào Diễn đàn công khai chưa thấy bài viết.
    * *Bước 2:* Dùng tài khoản Staff vào `/moderation/posts`, bấm nút "Phê duyệt".
    * *Kỳ vọng:* Bài xuất hiện ngay trên Diễn đàn chung.

- [ ] **`[DEV-B-SP3-03]` Tương Tác Like, Bình Luận & Báo Cáo Vi Phạm (Post Reports)**
  * **File:** `module/feed/controller/PostInteractionController.java`, `templates/moderation/reports-list.html`
  * **Ràng buộc nghiệp vụ:** `BRULE-POST-004`, `BRULE-POST-005`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* người dùng phát hiện bài viết có ngôn từ xúc phạm.
    * *When* nhấn "Báo cáo bài viết", chọn lý do `INAPPROPRIATE_LANGUAGE`.
    * *Then* hệ thống lưu vào `post_reports`. Cán bộ truy cập Dashboard Báo cáo có thể chọn "Ẩn bài viết" hoặc "Khóa tài khoản vi phạm".
  * **Manual Test Procedure:**
    * *Input:* Bấm nút Báo cáo vi phạm trên bài viết.
    * *Kỳ vọng UI:* Hiển thị thông báo "Cảm ơn bạn đã gửi báo cáo vi phạm". Cán bộ vào `/moderation/reports` thấy xuất hiện báo cáo mới.

- [ ] **`[DEV-B-SP3-04]` Tiếp Nhận Video Tự Động Từ Microservice Node.js Qua Webhook**
  * **File:** `module/integration/controller/NodejsWebhookController.java`, `module/integration/service/WebhookSecurityService.java`
  * **Ràng buộc nghiệp vụ:** `BRULE-NODE-001`
  * **Acceptance Criteria (Given - When - Then):**
    * *Given* Microservice Node.js gửi HTTP POST tới `/api/v1/integration/video-webhook` kèm header `X-Webhook-Signature` hợp lệ.
    * *When* Spring Boot xác thực HMAC-SHA256 thành công.
    * *Then* hệ thống tự động thêm bản ghi vào `post_attachments` với `file_type = 'MP4'`, `source_type = 'NODEJS_WEBHOOK'` và đính kèm vào bài viết tương ứng.
  * **Manual Test Procedure:**
    * *Input:* Gửi POST request với Payload JSON: `{"postId": 1, "videoUrl": "https://res.cloudinary.com/.../video.mp4", "durationSeconds": 120}` và Header Signature.
    * *Kỳ vọng:* Trả về HTTP 200 `{"success": true}`. Trên giao diện bài viết ID 1 xuất hiện trình phát Video MP4.

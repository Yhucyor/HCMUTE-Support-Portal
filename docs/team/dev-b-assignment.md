# BẢNG PHÂN BỔ TRÁCH NHIỆM & KHÓA HỢP ĐỒNG CODE: DEV B

* **Kỹ sư đảm nhiệm:** Dev B (Frontend/Fullstack & Feed/Integration Specialist)
* **Phạm vi trách nhiệm:** Module Bảng tin chính thức (Staff đăng thông báo kèm PDF/Word/Excel/MP4), Diễn đàn sinh viên (Feed, PENDING_APPROVAL, Comment, Like), Xử lý Báo cáo vi phạm (Report), Storage Service (Cloudinary & Local), Tích hợp Node.js Video Webhook.
* **Tài liệu tham chiếu:** [docs/brief.md](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/docs/brief.md) và [docs/team/engineering-rules.md](file:///d:/CauHinh_Java/workspace_sts/luyentap/doancuoiki_demo1/docs/team/engineering-rules.md)

---

## 1. Quyền Sở Hữu Tuyệt Đối (Ownership Package & Files)

> [!IMPORTANT]
> **Quy tắc bất khả xâm phạm:** Dev B nắm toàn quyền phát triển và bảo trì các package sau. Dev A tuyệt đối không sửa đổi hoặc commit vào các package này mà không có sự đồng thuận.

```text
com.school.counseling
  ├── common/storage/                   (IStorageService, CloudinaryStorageServiceImpl, LocalStorageServiceImpl)
  ├── config/
  │     ├── StorageConfig.java          (Cấu hình Cloudinary Bean & Local Storage)
  │     └── WebMvcConfig.java           (Resource Handlers cho Local Uploads)
  ├── module/
  │     ├── feed/                       (Toàn bộ Controller, Service, DTO, Repository cho Post, Comment, Reaction, Moderation, Reports)
  │     └── integration/                (NodejsWebhookController, WebhookSecurityService, VideoAttachmentService)
  └── templates/
        ├── layout/                     (main.html, header.html, footer.html, navbar.html)
        ├── feed/                       (official-list.html, official-detail.html, forum.html, create-post.html)
        ├── moderation/                 (pending-posts.html, reports-list.html)
        └── error/                      (403.html, 404.html, 500.html)
```

---

## 2. Locked Interface Contract (Hợp Đồng Code Khóa Cứng Cho Dev B)

Dev B bắt buộc phải triển khai hoặc cung cấp các Interface chuẩn sau để Dev A và toàn hệ thống sử dụng:

### 2.1. `IStorageService` (Dịch vụ Lưu trữ Tệp Tin Đa Nền Tảng)
```java
package com.school.counseling.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface IStorageService {
    StorageResult uploadFile(MultipartFile file, String targetFolder);
    boolean deleteFile(String fileUrlOrPublicId);
    String getPublicUrl(String fileIdentifier);

    record StorageResult(
        String originalFileName,
        String publicUrl,
        String fileType, // PDF, DOCX, XLSX, MP4, IMAGE
        long fileSizeBytes,
        boolean isSuccess,
        String errorMessage
    ) {}
}
```

### 2.2. `IModerationService` (Dịch vụ Phê Duyệt Bài Viết & Báo Cáo Vi Phạm)
```java
package com.school.counseling.module.feed.service;

import java.util.List;

public interface IModerationService {
    void approvePost(Long postId, Long reviewerUserId);
    void rejectPost(Long postId, Long reviewerUserId, String reason);
    void reportPost(Long postId, Long reporterUserId, String reason, String details);
    void resolveReport(Long reportId, Long reviewerUserId, String resolutionAction); // KEEP_POST, HIDE_POST, LOCK_USER
    long countPendingPostsByDepartment(Long departmentId);
}
```

### 2.3. `INodejsVideoWebhookService` (Tiếp Nhận Video Tự Động Từ Microservice Node.js)
```java
package com.school.counseling.module.integration.service;

public interface INodejsVideoWebhookService {
    boolean verifyWebhookSignature(String signatureHeader, String requestBody);
    void processVideoRenderedWebhook(VideoWebhookPayload payload);

    record VideoWebhookPayload(
        String signature,
        Long postId,
        String videoUrl,
        String thumbnailUrl,
        int durationSeconds,
        long fileSizeBytes
    ) {}
}
```

---

## 3. Danh Mục Deliverables Của Dev B Theo Sprint

* **Sprint 1:**
  * Xây dựng layout chuẩn tối giản (`layout/main.html`, `navbar.html`, `footer.html`) bằng Bootstrap 5 nguyên bản, đảm bảo màu sắc trang nhã dịu mắt (#f8f9fa, #1e3a8a).
  * Triển khai `IStorageService` (Dual mode: Cloudinary + Local) kèm kiểm tra Magic Bytes chống tấn công upload.
  * Thiết kế các trang lỗi tập trung (`403.html`, `404.html`, `500.html`).
* **Sprint 3:**
  * Xây dựng Module Bảng tin chính thức (Staff đăng bài đính kèm PDF, Word, Excel, MP4).
  * Diễn đàn sinh viên (Dòng thời gian, đăng bài ở trạng thái `PENDING_APPROVAL`, Like, Comment nhiều cấp).
  * Màn hình Kiểm duyệt bài viết (`/moderation/posts`) và Trung tâm xử lý Báo cáo vi phạm (`/moderation/reports`).
  * Endpoint Webhook `/api/v1/integration/video-webhook` bảo mật HMAC-SHA256 nhận Video từ Node.js.
* **Sprint 5:**
  * Đồng bộ và tối ưu UI/UX toàn hệ thống, tinh chỉnh CSS Bootstrap chuẩn.
  * Phối hợp với Dev A kiểm thử tích hợp (E2E Testing).

# 📋 KẾ HOẠCH & THIẾT KẾ TOÀN DIỆN (TIENDO8)
## TÍCH HỢP TOÀN DIỆN MODULE "BẢNG TIN THÔNG BÁO CHÍNH THỨC" (OFFICIAL FEED) & ĐỒNG BỘ HEADER TOÀN HỆ THỐNG

- **Ngày lập:** 23-10-2026
- **Module:** Bảng tin thông báo chính thức (`/feed/official`) & Header Navigation System
- **Tác giả:** Dev A (Backend Lead & Database Architect) phối hợp cùng Dev B (Frontend Lead & UI/UX)
- **Tài liệu liên quan:** [tiendo6.md](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/docs/tiendo/tiendo6.md), [tiendo7.md](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/docs/tiendo/tiendo7.md)

---

## 📌 I. HIỆN TRẠNG KỸ THUẬT & VẤN ĐỀ CẦN GIẢI QUYẾT

### 1. Hiện trạng Backend & Dữ liệu
* **Mã nguồn Backend đã hoàn chỉnh 100%:**
  - Controller [`OfficialFeedController.java`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/feed/controller/OfficialFeedController.java) xử lý đầy đủ các endpoint `/feed/official` (danh sách thông báo, xem chi tiết, lọc theo đơn vị, phân trang, ghim bài viết).
  - Tầng Service [`PostService.java`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/feed/service/PostService.java) đã có logic lọc bài viết chính thức đã phê duyệt (`getApprovedOfficialPosts`), xem chi tiết kèm tệp đính kèm và nhúng video hướng dẫn.
  - Phân quyền chặt chẽ: Cán bộ/Quản trị viên (`ROLE_STAFF`, `ROLE_ADMIN`) có quyền đăng bài thông báo mới tại `/feed/official/create`, ghim thông báo quan trọng; Sinh viên và khách vãng lai chỉ có quyền xem và tải văn bản.
* **Cơ sở dữ liệu Database:**
  - Đang có sẵn **9 bài viết thông báo công văn/quy chế thực tế** từ các Phòng Ban/Khoa (Tuyển sinh thạc sĩ, Sổ tay học vụ, Gia hạn upload quy trình, Nghĩa vụ học phí, Điểm I...).
  - Kiểm tra thực tế: Endpoint `GET http://localhost:8080/feed/official` trả về **HTTP 200** với đầy đủ dữ liệu.

### 2. Các điểm đứt gãy trên Giao diện (Frontend)
1. **Header bị khuyết mục "Bảng tin":**
   - Khi tái thiết kế lại thanh điều hướng chuẩn HCMUTE (Academic Horizon) trên các trang trung tâm ([`index.html`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/index.html), [`forum.html`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/feed/forum.html), [`faqs.html`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/faqs.html)), thanh Menu Desktop và Mobile Drawer chỉ có:
     `Trang chủ` | `Diễn đàn` | `Hỗ trợ AI` | `Tra cứu FAQ` | `Ticket hỗ trợ`.
   - **Mục "Bảng tin" (`/feed/official`) bị bỏ sót hoàn toàn**, khiến người dùng lầm tưởng module này chưa được đưa vào dự án.
2. **Lệch pha giao diện giữa các phân hệ (Design Mismatch):**
   - Các trang Bảng tin cũ ([`official-list.html`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/feed/official-list.html), [`official-detail.html`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/feed/official-detail.html)) vẫn đang dùng khung Bootstrap 5 cổ điển (`layout/main :: layout`), trong khi Trang chủ, Diễn đàn và FAQ đã chuyển sang phong cách **Tailwind CSS phẳng, hiện đại, phối màu xanh Oxford Blue `#003882` và đỏ Crimson `#991b1e` của HCMUTE**.
   - Cần hiện đại hóa giao diện Bảng tin để toàn bộ hệ thống đạt độ nhất quán mỹ thuật 100%.

---

## 🎯 II. PHƯƠNG ÁN THỐNG NHẤT VÀ KIẾN TRÚC TÍCH HỢP

### 1. Đồng bộ Thanh Header Toàn Hệ Thống
Bổ sung liên kết **"Bảng tin"** (`th:href="@{/feed/official}"`) vào vị trí trang trọng giữa *Trang chủ* và *Diễn đàn* trên tất cả các trang chính:

```
+----------------------------------------------------------------------------------------------------------+
|  [Logo HCMUTE]   Trang chủ  |  BẢNG TIN  |  Diễn đàn  |  Hỗ trợ AI  |  Tra cứu FAQ  |  Ticket hỗ trợ       |
+----------------------------------------------------------------------------------------------------------+
```

* **Thứ tự Menu chuẩn:**
  1. **Trang chủ** (`/`)
  2. ⭐ **Bảng tin** (`/feed/official`): Cổng thông báo, công văn chính thức từ Nhà trường & các Khoa/Phòng.
  3. **Diễn đàn** (`/feed/forum`): Không gian kết nối, thảo luận và hỏi đáp giữa sinh viên với nhau.
  4. **Hỗ trợ AI** (`/ai-chat`): Trợ lý tư vấn AI trực tuyến 24/7.
  5. **Tra cứu FAQ** (`/faqs`): 349 câu hỏi giải đáp quy chế đào tạo, học phí, tốt nghiệp.
  6. **Ticket hỗ trợ** (`/tickets/my-tickets`): Kênh gửi yêu cầu hỗ trợ chuyên sâu tới Cán bộ.

* **Hiển thị trạng thái Active thông minh:**
  - Khi đang ở `/feed/official`: Mục **Bảng tin** sẽ có gạch chân đỏ Crimson `#991b1e` và chữ đậm (`border-b-2 border-[#991b1e] text-[#991b1e]`).
  - Khi ở `/feed/forum`: Mục **Diễn đàn** active.

---

### 2. Tái cấu trúc Giao diện Bảng tin (`official-list.html`) theo Chuẩn HCMUTE
Chuyển đổi hoàn toàn từ Bootstrap 5 sang Tailwind CSS đồng bộ với `forum.html` và `faqs.html`:

#### A. Banner Nhận Diện Chính Thức
- Banner đầu trang trang trọng với huy hiệu:
  `[Badge: KÊNH THÔNG BÁO CHÍNH THỨC HCMUTE]`
- Tiêu đề: **Bảng Tin Thông Báo Chính Thức**
- Phụ đề: *Văn bản quy chế, kế hoạch đào tạo, học bổng và thông báo học vụ từ Ban Giám hiệu, Phòng Đào tạo và các Khoa.*

#### B. Thanh Bộ Lọc & Tìm Kiếm Thông Minh
- Ô tìm kiếm từ khóa thông báo theo thời gian thực (Tiêu đề, số hiệu công văn).
- Dropdown lọc theo Khoa / Phòng ban (*Phòng Đào tạo, Phòng CTSV, Khoa CNTT, Khoa CKM...*).
- Tab chuyển đổi nhanh:
  + **Tất cả thông báo**
  + **Thông báo được ghim** (`📌 Pinned`)
  + **Nút chuyển nhanh sang Diễn đàn sinh viên**

#### C. Lưới Thẻ Bài Viết (Card UI Layout - 3 Cột Grid)
Mỗi thẻ thông báo được thiết kế sắc nét:
1. **Header thẻ:**
   - Badge tên Đơn vị/Khoa (*Phòng Đào tạo & CTSV*, *Khoa CNTT*...).
   - Huy hiệu `Được ghim` màu đỏ nổi bật nếu bài viết được ghim ưu tiên.
2. **Tiêu đề thông báo:** Font chữ đậm, hover đổi màu xanh thương hiệu `#003882`.
3. **Nội dung tóm tắt:** Trích đoạn ngắn gọn, sạch sẽ.
4. **Loại đính kèm (Media Badges):**
   - Icon PDF đỏ nếu có file công văn đính kèm.
   - Icon Video xanh nếu có video clip hướng dẫn.
5. **Footer thẻ:**
   - Ngày ban hành văn bản định dạng `dd/MM/yyyy`.
   - Lượt xem (`viewCount`).
   - Nút "Xem chi tiết" dẫn đến trang văn bản đầy đủ.

#### D. Nút Hành Động Phân Quyền (RBAC Button)
- Nếu là **Cán bộ / Quản trị viên** (`STAFF / ADMIN`): Hiển thị nút bấm to rõ `+ Đăng Thông Báo Mới` dẫn tới `/feed/official/create`.
- Nếu là **Sinh viên**: Có nút gợi ý `"Bạn có câu hỏi? Đăng bài thảo luận tại Diễn đàn"`.

---

### 3. Tái cấu trúc Trang Chi Tiết Thông Báo (`official-detail.html`)
- Hiển thị toàn văn nội dung thông báo chính thức.
- **Trình nhúng tài liệu trực quan (Document Viewer):**
  - Khung xem trước file PDF/Word đính kèm trực tiếp trên trình duyệt.
  - Nút tải tệp đính kèm nhanh (`Download Attachment`).
- **Trình phát Video (Video Player):**
  - Nếu thông báo có video YouTube / Google Drive, hiển thị responsive iframe phát trực tiếp.
- Breadcrumb điều hướng: `Trang chủ` $\rightarrow$ `Bảng tin thông báo` $\rightarrow$ `Tên bài viết`.
- Nút "In văn bản" (`window.print()`) và nút "Chia sẻ liên kết".

---

## 👥 III. PHÂN CHIA NHIỆM VỤ CHI TIẾT (DEV A VS DEV B)

### 🛠️ 1. Nhiệm vụ của Dev A (Backend Lead & Database Architect)
1. **Kiểm tra & Bổ sung API / Controller Endpoint:**
   - Rà soát các hàm trong [`OfficialFeedController.java`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/feed/controller/OfficialFeedController.java), đảm bảo truyền đúng các thuộc tính sang Model:
     + `posts`: Danh sách phân trang `Page<PostResponseDto>`.
     + `departments`: Danh sách Khoa/Phòng để lọc.
     + `pinnedPosts`: Danh sách các thông báo được ghim lên đầu.
   - Bổ sung hàm toggle ghim `/feed/official/toggle-pin/{id}` cho Admin.
2. **Bổ sung Dữ liệu Mẫu (Data Seeder):**
   - Đảm bảo có từ 9 - 12 bài viết thông báo chính thức đa dạng từ các đơn vị: Phòng Đào tạo (A1-201), Phòng CTSV, Phòng Kế hoạch Tài chính, Khoa CNTT, Trung tâm Ngoại ngữ.

### 🎨 2. Nhiệm vụ của Dev B (Frontend Lead & UI/UX Specialist)
1. **Cập nhật Header & Mobile Drawer:**
   - Bổ sung liên kết `Bảng tin` vào thanh menu của:
     - `src/main/resources/templates/index.html`
     - `src/main/resources/templates/feed/forum.html`
     - `src/main/resources/templates/faqs.html`
2. **Thiết kế lại 2 template Bảng tin:**
   - `src/main/resources/templates/feed/official-list.html`: Viết lại hoàn toàn bằng Tailwind CSS chuẩn Academic Horizon.
   - `src/main/resources/templates/feed/official-detail.html`: Thiết kế trang đọc văn bản chính thức trang trọng, chuyên nghiệp.

---

## 📅 IV. KẾ HOẠCH TRIỂN KHAI THEO TỪNG GIAI ĐOÀN

| Giai đoạn | Nội dung công việc | Phụ trách | Trạng thái |
| :--- | :--- | :---: | :---: |
| **Giai đoạn 1** | Bổ sung mục "Bảng tin" vào Header Desktop & Mobile Drawer trên `index.html`, `forum.html`, `faqs.html` | **Dev B** | ⏳ Chờ thực hiện |
| **Giai đoạn 2** | Hiện đại hóa `official-list.html` theo chuẩn Tailwind CSS HCMUTE (Card Grid, Filter Khoa, Badge đính kèm) | **Dev B** | ⏳ Chờ thực hiện |
| **Giai đoạn 3** | Hiện đại hóa `official-detail.html` (Xem văn bản chi tiết, trình xem file đính kèm & video player) | **Dev B** | ⏳ Chờ thực hiện |
| **Giai đoạn 4** | Kiểm tra phân quyền Cán bộ (Đăng thông báo, ghim bài) và kiểm thử toàn bộ liên kết điều hướng | **Dev A & B** | ⏳ Chờ thực hiện |
| **Giai đoạn 5** | Biên dịch `mvn test-compile`, kiểm thử live trên trình duyệt và push commit lên Git | **Dev A** | ⏳ Chờ thực hiện |

# 📋 KẾ HOẠCH & THIẾT KẾ TOÀN DIỆN (TIENDO7)
## TỐI ƯU HÓA DIỄN ĐÀN: HỢP NHẤT BỘ SOẠN THẢO, TƯƠNG TÁC THỜI GIAN THỰC & ĐÁNH GIÁ RICH TEXT EDITOR

- **Ngày lập:** 23-10-2026
- **Module:** Diễn đàn sinh viên HCMUTE (`/feed/forum`)
- **Tác giả:** Dev A (Backend Lead & Database Architect) phối hợp cùng Dev B (Frontend Lead & UI/UX)
- **Tài liệu liên quan:** [tiendo6.md](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/docs/tiendo/tiendo6.md)

---

## 📌 I. HIỆN TRẠNG & CÁC VẤN ĐỀ CẦN GIẢI QUYẾT

### 1. Sự phân mảnh giữa 2 cơ chế đăng bài
Hiện tại hệ thống đang tồn tại song song 2 cách tạo bài viết với trải nghiệm người dùng bị đứt gãy:
* **Cơ chế 1 (Trên bảng tin Feed):** Một thẻ nhỏ (`Post Composer Card`) có avatar và ô input *"Bạn đang muốn chia sẻ hay đặt câu hỏi học tập điều gì?"*. Nút *"Mở nhanh"* gọi hàm `openCreateModal()` nhưng hàm này **chưa hề được định nghĩa trong mã nguồn**. Các nút còn lại (*Ảnh/Video*, *Tài liệu*, *Hỏi đáp*, *Đăng bài*) đều là thẻ liên kết điều hướng cứng sang trang `/feed/forum/create`.
* **Cơ chế 2 (Trang tạo bài viết riêng `create-post.html`):** Một trang độc lập khổng lồ dài **1,220 dòng HTML (86 KB)**. Quá nhiều thành phần thừa thãi và rườm rà:
  - Widget chọn phạm vi hiển thị (Công khai, Khoa, Riêng tư) nhưng nghiệp vụ thực tế bài viết sinh viên chỉ cần duyệt công khai hoặc gắn theo Khoa.
  - Thanh công cụ định dạng tự chế bằng `document.execCommand` cũ kỹ, dễ phát sinh lỗi trên các trình duyệt hiện đại.
  - Hệ thống thẻ tag nhiều bước chọn, hộp thoại auto-save draft lưu tạm localStorage không đồng bộ với máy chủ.
  - Bắt buộc người dùng phải tải một trang mới hoàn toàn rồi redirect ngược lại Feed sau khi tạo bài.

### 2. Trải nghiệm tương tác bình luận & lượt thích bị gián đoạn
* **Không xem được danh sách bình luận:** Trên mỗi thẻ bài viết tại `forum.html`, người dùng chỉ thấy nhãn số lượng `X Thảo luận` và một ô nhập bình luận nhanh. **Template hoàn toàn không render danh sách các bình luận đã có**, khiến sinh viên và giảng viên không thể đọc được nội dung trao đổi bên dưới bài viết.
* **Tương tác Like gây reload giật màn hình:** Nút thả tim đang được bọc trong thẻ `<form method="post" action="/feed/forum/{id}/like">`. Mỗi lần bấm thích/bỏ thích, toàn bộ trang web bị tải lại và giật xuống vị trí anchor `#post_{id}`, tạo cảm giác khó chịu và làm giảm trải nghiệm người dùng.

### 3. Câu hỏi về việc tích hợp TinyMCE (Tiny)
* Người dùng đặt vấn đề: Có nên đưa trình soạn thảo mã nguồn mở Tiny (TinyMCE) vào diễn đàn hay không?

---

## 🎯 II. GIẢI PHÁP HỢP NHẤT 2 CƠ CHẾ ĐĂNG BÀI: "SMART IN-FEED COMPOSER & MODAL"

Thay vì duy trì 2 giao diện rời rạc (1 card vô dụng trên Feed + 1 trang 1,220 dòng nặng nề), ta **hợp nhất thành một luồng đăng bài duy nhất ngay tại Diễn đàn**:

### 1. Mô hình hoạt động (UX Flow)
```
[Tại Bảng Tin Feed]
+---------------------------------------------------------------------------------+
| [Avatar]  Bạn đang muốn chia sẻ hay đặt câu hỏi học tập điều gì?   [Icon Edit]   |
| [Ảnh/Đính kèm]  [Khoa/Phòng]  [Chủ đề]                       [ Nút Đăng Bài ]   |
+---------------------------------------------------------------------------------+
                                      |
         Click vào ô soạn thảo / bấm "Đăng bài" / "Ảnh"
                                      v
+---------------------------------------------------------------------------------+
|                       MODAL TẠO BÀI VIẾT NHANH (TINH GỌN)                       |
|---------------------------------------------------------------------------------|
| 1. Header tác giả: Avatar + Tên sinh viên + Badge Khoa                          |
| 2. Gắn với Khoa/Phòng: [Dropdown chọn Khoa hoặc Toàn trường]                   |
| 3. Tiêu đề bài viết: <input> ngắn gọn, nổi bật (Placeholder rõ ràng)             |
| 4. Nội dung bài viết: Editor thông minh, tự động giãn dòng (Auto-expanding)    |
| 5. Đính kèm hình ảnh: Drag & Drop hoặc click chọn file -> Preview ảnh tức thì   |
| 6. Footer: [Hủy bỏ]                          [ Nút Đăng bài thảo luận ]         |
+---------------------------------------------------------------------------------+
```

### 2. Ưu điểm vượt trội của giải pháp
1. **Không rời trang (Zero Page Navigation):** Người dùng đang lướt bảng tin có thể mở modal đăng bài trong 0.1 giây, nhấn đăng và bài viết mới xuất hiện ngay trên đầu trang.
2. **Tiết kiệm mã nguồn & Tài nguyên:** Giảm từ 1,220 dòng HTML ở trang riêng xuống còn khoảng 80 dòng HTML Modal dùng chung ngay trong layout, tải trang nhanh hơn gấp 5 lần.
3. **Thân thiện với thiết bị di động:** Modal thích ứng linh hoạt (Bottom Sheet trên Mobile, Centered Modal trên Desktop).
4. **Trang `/feed/forum/create`:** Giữ nguyên route nhưng tái sử dụng đúng component gọn này để làm trang fallback nếu người dùng truy cập trực tiếp bằng URL.

---

## 💬 III. GIẢI PHÁP TƯƠNG TÁC THỜI GIAN THỰC (BÌNH LUẬN & LIKE KHÔNG RELOAD)

### 1. Xem và Quản lý Bình luận dạng Accordion trượt mượt mà (Comment Drawer)
* **Cơ chế:** Nút `X Thảo luận` (`chat_bubble`) chuyển thành nút bấm có thuộc tính `data-post-id="{id}"`.
* **Hành vi khi click:**
  - Toggle hiển thị khối bình luận bên dưới bài viết (`#comments-container-{id}`) với hiệu ứng trượt nhẹ (accordion/transition).
  - Tự động tải/hiển thị danh sách bình luận đã có:
    + Avatar tác giả bình luận.
    + Tên người bình luận + Badge chức danh (*Sinh viên*, *Giảng viên*, *Cán bộ Phòng Đào tạo*).
    + Thời gian đăng định dạng thân thiện (`15 phút trước`, `Hôm qua 14:30`...).
    + Nội dung bình luận giữ nguyên định dạng xuống dòng.
* **Gửi bình luận không tải lại trang:**
  - Bắt sự kiện `submit` của form bình luận qua JavaScript `fetch()`.
  - Khi gửi thành công: Tạo một phần tử bình luận mới chèn vào cuối danh sách (DOM append), tăng chỉ số đếm bình luận lên +1 và xóa trắng ô nhập liệu (Clear input) mà không reload trang.

### 2. Thả tim (Like) tức thì qua REST API (Optimistic UI Update)
* **Loại bỏ form submit truyền thống**, chuyển sang cơ chế bất đồng bộ Fetch API:
  - Endpoint: `POST /feed/forum/{id}/like` (trả về JSON hoặc xử lý nhẹ qua AJAX).
* **Trải nghiệm tức thì (Optimistic UI):**
  1. Khi người dùng click nút Thích:
     - Biểu tượng tim lập tức chuyển màu đỏ rực (`text-rose-600`, icon fill 1) và phóng to nhẹ rồi trở về kích thước ban đầu (hiệu ứng nhịp tim `scale-125 transition-transform`).
     - Con số đếm tăng lên +1 (hoặc giảm -1 nếu bỏ thích).
  2. Ngầm bên dưới, JavaScript gửi `fetch('/feed/forum/' + postId + '/like', { method: 'POST', headers: { ... } })`.
  3. Nếu có lỗi mạng hoặc phiên đăng nhập hết hạn: Tự động hoàn tác trạng thái tim và hiển thị thông báo nhẹ (Toast thông báo).

---

## ⚖️ IV. ĐÁNH GIÁ CHUYÊN SÂU: CÓ NÊN TÍCH HỢP MÃ NGUỒN MỞ TINYMCE?

### 1. Phân tích điểm mạnh & Hạn chế của TinyMCE đối với dự án này

| Tiêu chí | Đánh giá TinyMCE (Tiny) | Nhận xét đối với Diễn đàn HCMUTE |
| :--- | :--- | :--- |
| **Bản quyền & CDN** | Từ phiên bản 6 và 7, TinyMCE yêu cầu **API Key bắt buộc**. Nếu dùng Cloud không có key sẽ hiển thị popup và viền cảnh báo màu đỏ *"TinyMCE domain not registered"* rất khó chịu. | ❌ Rất bất tiện cho dự án sinh viên/học tập và môi trường nội bộ. |
| **Dung lượng (Bundle Size)** | Tự host trọn gói bundle TinyMCE nặng từ **600 KB đến 1.5 MB** kèm theo hàng chục file phụ trợ (themes, skins, plugins, icons). | ❌ Quá nặng, làm chậm thời gian tải trang ban đầu của Portal. |
| **Giao diện & Thẩm mỹ** | Thiết kế theo hướng thanh công cụ Office/Word truyền thống, bọc trong thẻ `<iframe>` độc lập. | ❌ Không ăn nhập với phong cách thiết kế phẳng hiện đại (Tailwind CSS, Material Design 3, HCMUTE Blue/Red). |
| **Độ phức tạp trên Mobile** | Thanh công cụ nhiều hàng dễ vỡ giao diện trên màn hình điện thoại; khó tối ưu responsive. | ❌ Trải nghiệm gõ bài trên thiết bị di động rất bất tiện. |
| **Nhu cầu thực tế** | Sinh viên lên diễn đàn chủ yếu đăng câu hỏi ngắn (1 - 3 đoạn văn), chia sẻ tài liệu, gắn 1 - 2 ảnh và mã nguồn code. | ⚠️ **TinyMCE là giải pháp "Overkill"** (quá tải cho nhu cầu thực tế). |

### 2. Đề xuất Giải pháp Thay thế Tối ưu hơn TinyMCE

#### 🌟 Lựa chọn 1 (Khuyên dùng số 1 - Tối ưu nhất): Tích hợp **Quill.js** (Phiên bản Bubble hoặc Snow tối giản)
- **Đặc điểm:** Thư viện WYSIWYG mã nguồn mở hàng đầu thế giới (BSD License), **100% miễn phí vĩnh viễn, không cần bất kỳ API Key nào**.
- **Dung lượng:** Cực kỳ nhỏ nhẹ (~40 KB gzipped).
- **Giao diện:** Tinh gọn, phẳng, hỗ trợ theme tối giản ăn khớp hoàn toàn với Tailwind CSS.
- **Tính năng vừa đủ:** Bold, Italic, Link, Trích dẫn, Khối mã nguồn (`Code Block`), Chèn ảnh trực quan.

#### ⚡ Lựa chọn 2 (Siêu nhẹ - 0KB Thư viện): Custom Smart Textarea với Quick Action Toolbar
- **Đặc điểm:** Sử dụng thẻ `<textarea>` kết hợp JavaScript tự co giãn (`auto-resize`) cùng thanh công cụ nút bấm nhanh:
  - Các nút định dạng: In đậm (`**text**`), In nghiêng (`*text*`), Khối mã nguồn (` ```code``` `), Gắn link.
  - Vùng kéo thả tệp hình ảnh đính kèm (Drag & Drop image upload với preview thumbnail).
- **Ưu điểm:** Tải trang 0ms, không phụ thuộc bất kỳ thư viện JS bên thứ ba nào, kiểm soát hoàn toàn 100% code.

👉 **Kết luận:** **KHÔNG NÊN dùng TinyMCE**. Thay vào đó, giải pháp lý tưởng nhất cho diễn đàn sinh viên là sử dụng **Smart Textarea có Toolbar hỗ trợ Markdown & Drag-Drop Ảnh** (hoặc tích hợp **Quill.js** bản Snow tối giản).

---

## 🛠️ V. THIẾT KẾ KIẾN TRÚC & API (DEV A - BACKEND LEAD)

Dev A chịu trách nhiệm toàn bộ tầng dữ liệu, Controller và RESTful API:

### 1. Bổ sung REST API cho Tương tác Diễn đàn
| Method | Endpoint | Quyền hạn | Mô tả chức năng | Dữ liệu trả về (JSON) |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/feed/posts/{id}/like` | `isAuthenticated()` | Thả tim / Bỏ tim bài viết | `{ "success": true, "liked": true, "likeCount": 15 }` |
| `GET` | `/api/v1/feed/posts/{id}/comments` | `PermitAll` | Lấy danh sách bình luận của bài viết | `{ "success": true, "data": [CommentDto...] }` |
| `POST` | `/api/v1/feed/posts/{id}/comments` | `isAuthenticated()` | Gửi bình luận mới vào bài viết | `{ "success": true, "data": CommentDto }` |

### 2. Thiết kế DTOs mới
* **`CommentDto.java`:**
  - `Long id`, `Long postId`, `String content`, `LocalDateTime createdAt`
  - `Long authorId`, `String authorFullName`, `String authorAvatar`, `String authorRole`, `String departmentName`
* **`LikeToggleResponseDto.java`:**
  - `boolean liked`, `int likeCount`, `String message`

### 3. Tối ưu Controller Web (`ForumController.java`)
* Giữ nguyên khả năng hỗ trợ Server-side Rendering của Thymeleaf cho SEO và tải trang lần đầu.
* Đồng thời cung cấp endpoint trả về JSON khi có Header `X-Requested-With: XMLHttpRequest` hoặc gọi qua `/api/v1/feed/**` để phục vụ trải nghiệm người dùng không reload trang.

---

## 🎨 VI. THIẾT KẾ GIAO DIỆN & TƯƠNG TÁC (DEV B - FRONTEND LEAD)

Dev B chịu trách nhiệm xây dựng giao diện người dùng theo chuẩn thiết kế Portal HCMUTE:

### 1. Xây dựng Smart Modal Tạo Bài Viết (`modal-create-post.html` fragment)
- Kích thước: `max-w-2xl`, bo tròn `rounded-2xl`, hiệu ứng mở `backdrop-blur-sm`.
- Trường nhập liệu:
  + Dropdown Khoa/Phòng: giao diện viền xám nhẹ `border-slate-200`, focus viền xanh `#003882`.
  + Ô nhập tiêu đề: chữ to bản vừa vặn, không lỗi font tiếng Việt.
  + Ô nhập nội dung: Tự động tăng chiều cao theo số lượng dòng văn bản gõ vào (`scrollHeight`).
  + Vùng đính kèm ảnh: Kèm khung xem trước (Preview Thumbnail) với nút dấu nhân (`x`) để gỡ ảnh đã chọn trước khi gửi.

### 2. Thiết kế Khối Bình Luận (Comment Accordion)
- Mỗi bài viết có một container `#comments-list-{postId}` mặc định ở trạng thái ẩn (`hidden`).
- Khi click nút thảo luận: chuyển trạng thái thành hiển thị với animation trượt êm (`transition-all duration-300`).
- Mỗi comment item:
  + Avatar tròn kích thước nhỏ 32x32px.
  + Khối nội dung comment có nền màu xám nhạt `bg-slate-50 border border-slate-100 rounded-2xl p-3`.
  + Tên tác giả đậm nét, badge màu xanh cho Cán bộ/Giảng viên và màu xám cho Sinh viên.

### 3. Tách biệt File Script (`static/js/modules/forum-interactions.js`)
- Áp dụng kỹ thuật **Event Delegation** trên container danh sách bài viết:
  - Bắt sự kiện click nút like (`.btn-like-post`).
  - Bắt sự kiện click xem bình luận (`.btn-toggle-comments`).
  - Bắt sự kiện submit gửi bình luận nhanh (`.form-quick-comment`).
- Loại bỏ toàn bộ code JavaScript inline trong template HTML.

---

## 📅 VII. KẾ HOẠCH TRIỂN KHAI THEO TỪNG GIAI ĐOÀN

| Giai đoạn | Nội dung công việc | Phụ trách | Kết quả đầu ra |
| :--- | :--- | :---: | :--- |
| **Giai đoạn 1** | Viết REST Controller & Service cho Like/Comment Ajax; tạo `CommentDto` và `LikeResponseDto` | **Dev A** | API kiểm thử thành công qua Postman / cURL, trả về JSON chuẩn |
| **Giai đoạn 2** | Xây dựng Smart Modal đăng bài và Khối hiển thị bình luận trực quan trên `forum.html` | **Dev B** | Giao diện hiển thị danh sách bình luận sắc nét, modal mở nhanh mượt mà |
| **Giai đoạn 3** | Viết file JavaScript `forum-interactions.js` xử lý Fetch API cho Like, Comment và Preview ảnh | **Dev B** | Like không reload trang, gửi comment hiển thị tức thì |
| **Giai đoạn 4** | Kiểm thử tích hợp tổng thể, kiểm tra responsive mobile/desktop và push lên Git | **Dev A & B** | Trải nghiệm người dùng đồng nhất, không phát sinh lỗi |

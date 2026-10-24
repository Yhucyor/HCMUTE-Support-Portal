# 📋 KẾ HOẠCH & PHƯƠNG ÁN TỐI ƯU HÓA TOÀN DIỆN DIỄN ĐÀN SINH VIÊN (FORUM MODULE)

- **Ngày lập:** 23-10-2026
- **Module:** Diễn đàn sinh viên HCMUTE (`/feed/forum`)
- **Tác giả:** Dev A (Backend Lead & Database Architect) phối hợp phân chia công việc cùng Dev B (Frontend Lead)
- **Mục tiêu:**
  1. Kiểm tra & bóc tách hiện trạng dữ liệu trên trang Diễn đàn (phần lớn là dữ liệu cứng/mock để làm đẹp giao diện).
  2. Lên phương án thiết kế tinh gọn (Layout 2 cột hiện đại thay cho 3 cột rườm rà có resizer).
  3. Kế hoạch kết nối 100% dữ liệu thực từ Cơ sở dữ liệu JPA (Seeder dữ liệu thật, DTO, Repository).
  4. Phân công ranh giới trách nhiệm rõ ràng giữa Dev A (Backend) và Dev B (Frontend) theo quy chuẩn dự án.

---

## 🔍 I. HIỆN TRẠNG & BÓC TÁCH DỮ LIỆU CỨNG (MOCK / DUMMY DATA)

Qua rà soát chi tiết mã nguồn [forum.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/feed/forum.html) (1,318 dòng HTML) và [ForumController.java](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/feed/controller/ForumController.java), phát hiện **hơn 70% nội dung hiển thị trên giao diện là dữ liệu tĩnh (hardcoded dummy data)** để "làm đẹp màn hình":

| STT | Thành phần giao diện | Hiện trạng dữ liệu | Đánh giá kỹ thuật & Hệ quả |
| :--- | :--- | :--- | :--- |
| **1** | **Banner diễn đàn** | Chứa dòng tĩnh: `"1,482 bạn đang online"` | Số liệu ảo, không có WebSocket hay session counter thực tế theo dõi người dùng trực tuyến. |
| **2** | **Cột trái: Nhóm sinh viên nổi bật** | Chứa 5 nhóm tĩnh: *Cộng đồng SV HCMUTE (18.4k), Học tập & Thi cử (9.2k), CLB Lập trình (6.1k)...* | Cơ sở dữ liệu hoàn toàn **không có bảng Group/Nhóm**. Toàn bộ là thẻ `<a>` tĩnh với link `#`, không click tương tác được. |
| **3** | **Cột trái: Mini Metric Badge** | Chứa chỉ số tĩnh: `"Tỉ lệ giải đáp 96.8% - Trung bình phản hồi trong 18 phút"` | Dữ liệu vẽ cứng bằng HTML/CSS, không truy vấn từ bảng bài viết/bình luận. |
| **4** | **Cột giữa: Bài viết mẫu (Sample Posts)** | 2 bài viết mẫu tĩnh (*Trần Minh Quân - 412 likes, Phạm Văn Đức - 254 likes*) | Do [DataInitializer.java](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/config/DataInitializer.java) chưa seed bài viết diễn đàn (`post_type = 'STUDENT_FORUM'`), database rỗng nên template buộc phải hiển thị khối HTML mẫu này. Người dùng không thể like hay bình luận thật trên 2 bài này. |
| **5** | **Cột phải: Chủ đề sôi nổi (Trending)** | 4 hashtag tĩnh: `#DangKyHocPhanHK2 (1,420 bài), #TuyenDungThucTap2026 (890 bài)...` | Số lượng bài thảo luận ảo, không truy vấn từ cơ sở dữ liệu theo lượt tương tác. |
| **6** | **Cột phải: Sự kiện sắp diễn ra** | 2 sự kiện tĩnh: *Ngày hội việc làm (25/10/2026), Workshop AI (02/11/2026)* | Hệ thống không có bảng `events`, thẻ sự kiện không có liên kết điều hướng. |
| **7** | **Bộ kéo kéo thả 3 cột (Resizer JS)** | Hơn 150 dòng JS kéo thả `resizerLeft`, `resizerRight`, lưu `localStorage` | Cực kỳ cồng kềnh, dễ gây giật lag khi cuộn, làm vỡ layout trên thiết bị di động/máy tính bảng, thanh cuộn lồng thanh cuộn (`forum-scrollbar`). |
| **8** | **Mascot AI Chatbot Widget** | Chứa hơn 200 dòng JavaScript inline lặp lại nguyên xi từ trang chủ | Vi phạm quy tắc Clean Code & SoC (Separation of Concerns), làm phình to file template không cần thiết khi hệ thống đã có `/ai-chat`. |

---

## 📐 II. PHƯƠNG ÁN THIẾT KẾ MỚI: TINH GỌN, HIỆN ĐẠI & THỰC CHẤT

### 2.1. Tái cấu trúc Layout: Chuyển từ 3 cột phức tạp sang 2 cột tối ưu (Grid 8 / 4)
* **Loại bỏ hoàn toàn bộ Resizer kéo thả 3 cột:** Giúp trang tải nhẹ hơn 60%, cuộn mượt tự nhiên, đáp ứng chuẩn Responsive từ Mobile đến Màn hình lớn (Desktop).
* **Bố cục 2 Cột:**
  ```
  +-----------------------------------------------------------------------------------+
  |                                 NAVBAR CHUẨN                                      |
  +-----------------------------------------------------------------------------------+
  | BANNER GỌN: Tiêu đề Diễn đàn + Thống kê thực (Tổng số bài viết, Số người tham gia) |
  +-------------------------------------------------+---------------------------------+
  | CỘT CHÍNH (MAIN FEED - 8 COLS)                  | CỘT PHỤ (SIDEBAR - 4 COLS)      |
  | 1. Thanh soạn bài nhanh (Quick Post Bar)        | 1. Card Quy tắc cộng đồng ngắn  |
  | 2. Bộ lọc tinh gọn:                             | 2. Lọc theo Khoa / Phòng ban    |
  |    - Tabs: Mới nhất | Sôi nổi nhất | Của tôi    | 3. Chủ đề quan tâm nổi bật      |
  |    - Tìm kiếm từ khóa (Live Search)             | 4. Tiện ích học tập nhanh       |
  | 3. Danh sách Bài viết động từ Database (JPA)    |    (Thời khóa biểu, ĐKMH, FAQ)  |
  |    - Header tác giả, Khoa, Thời gian đăng       |                                 |
  |    - Tiêu đề & Nội dung thảo luận               |                                 |
  |    - Ảnh đính kèm (nếu có)                      |                                 |
  |    - Tương tác thật: Like Ajax, Comment Form    |                                 |
  | 4. Phân trang chuẩn 10 bài/trang                |                                 |
  +-------------------------------------------------+---------------------------------+
  ```

### 2.2. Chiến lược loại bỏ Dữ liệu rác & Thay thế bằng Dữ liệu thật
1. **Xóa bỏ các khối dữ liệu ảo:**
   - ❌ Xóa khối "Nhóm sinh viên nổi bật" (vì không có nghiệp vụ Groups trong đồ án).
   - ❌ Xóa khối "Sự kiện sắp diễn ra" tĩnh.
   - ❌ Xóa các con số ảo: "1,482 online", "Tỉ lệ 96.8%".
2. **Nạp dữ liệu mẫu thực tế vào Database qua DataInitializer:**
   - Tạo từ 10 - 15 bài viết sinh viên phong phú, sát thực tế học tập tại HCMUTE:
     + Chia sẻ kinh nghiệm làm đồ án tốt nghiệp / đồ án môn học các khoa (CNTT, CKM, Điện - Điện tử...).
     + Thảo luận chuẩn đầu ra ngoại ngữ TOEIC / AVĐR tháng 11, tháng 12.
     + Kinh nghiệm chọn môn học, né trùng lịch thi kết thúc học phần.
     + Tuyển thành viên tham gia Nghiên cứu khoa học sinh viên (NCKH) cấp trường.
     + Hướng dẫn thủ tục xin cấp bảng điểm, giấy xác nhận sinh viên tại Phòng Đào tạo.
   - Kèm các bình luận mẫu (`Comment`) trả lời trao đổi qua lại giữa các tài khoản sinh viên và giảng viên, giúp diễn đàn có sức sống thực tế ngay khi khởi động ứng dụng.

---

## 👥 III. PHÂN CHIA NHIỆM VỤ CHI TIẾT (DEV A VS DEV B)

Tuân thủ nghiêm ngặt **Quy tắc phân chia vai trò** giữa Backend Lead và Frontend Lead:

### 🛠️ 1. Nhiệm vụ của Dev A (Backend Lead & Database Architect)
1. **Cơ sở dữ liệu & Data Seeder (`DataInitializer.java`):**
   - Viết phương thức `seedStudentForumPosts()`:
     - Tạo 10 - 15 bài viết `Post` với `postType = "STUDENT_FORUM"`, `status = "APPROVED"`.
     - Gắn tác giả là các user sinh viên thực trong hệ thống thuộc nhiều khoa khác nhau.
     - Tạo 25+ bình luận trao đổi thực tế trong bảng `post_comments`.
     - Cập nhật số lượt thích `Like` và `viewCount` hợp lý.
2. **Tối ưu Tầng Nghiệp Vụ (`PostService.java` & `PostRepository.java`):**
   - Viết query lấy danh sách bài viết thảo luận sinh viên sắp xếp theo:
     + **Mới nhất (Latest):** Sắp xếp theo `createdAt DESC`.
     + **Sôi nổi nhất (Popular):** Sắp xếp theo `(p.likeCount + p.commentCount * 2) DESC, p.createdAt DESC`.
   - Cung cấp phương thức thống kê diễn đàn thực: tổng số bài viết đã duyệt, tổng số lượt bình luận.
3. **Tối ưu Tầng Controller (`ForumController.java`):**
   - Truyền dữ liệu phân trang sạch qua `PostResponseDto` sang Model.
   - Bổ sung Model Attribute: `forumStats` (tổng bài viết thực tế), `departments` (danh sách khoa hoạt động).
   - Đảm bảo các endpoint Like, Comment, Xóa bài hoạt động an toàn theo CSRF và bảo mật RBAC.

### 🎨 2. Nhiệm vụ của Dev B (Frontend Lead & UI/UX Specialist)
1. **Tái cấu trúc Template `templates/feed/forum.html`:**
   - Dỡ bỏ cấu trúc 3 cột có resizer kéo thả phức tạp; chuyển thành layout 2 cột Grid (8/4) sạch đẹp, chuẩn responsive.
   - Xóa bỏ toàn bộ các thẻ HTML chứa bài viết mẫu tĩnh (Sample Posts) và các widget giả (Nhóm sinh viên fake, sự kiện fake).
   - Render danh sách bài viết từ biến `posts` của Backend, hiển thị ảnh tác giả, badge khoa, ngày đăng, nội dung, danh sách bình luận lồng nhau.
2. **Tách biệt JavaScript (`static/js/modules/forum.js`):**
   - Chuyển toàn bộ logic xử lý Modal tạo bài viết, toggle phần bình luận, gửi like AJAX và validation vào file script riêng.
   - Tối ưu hiệu ứng hover card, badge khoa, nút like hiệu ứng trái tim đập nhẹ.

---

## 📅 IV. LỘ TRÌNH THỰC HIỆN TỪNG BƯỚC

- [x] **Bước 1 (Dev A):** Viết Seeder bài viết và bình luận diễn đàn thực tế trong `DataInitializer.java` (12 bài viết phong phú các khoa CKM, CNTT, Điện - Điện tử, Kinh tế, Ngoại ngữ + 20+ bình luận trao đổi mồi thực tế + ảnh minh họa từ bài mẫu cũ). Kiểm tra `mvn test-compile` thành công.
- [x] **Bước 2 (Dev A & Dev B):** Loại bỏ toàn bộ dữ liệu mock ảo trên giao diện `forum.html` (Xóa khối Nhóm sinh viên fake, xóa khối Sự kiện sắp diễn ra tĩnh, xóa số liệu 1,482 online, xóa bài mẫu tĩnh).
- [x] **Bước 3 (Kiểm thử):** Đã khởi động ứng dụng và kiểm chứng `GET /feed/forum` trả về 12 bài viết động 100% từ Database JPA.
- [ ] **Bước 4 (Dev A):** Bổ sung sorting tab "Sôi nổi nhất" (`popular`) theo `(likeCount + commentCount * 2)` trong `PostRepository` & `PostService`.
- [ ] **Bước 5 (Dev B):** Tinh gọn script giao diện sang `forum.js`.

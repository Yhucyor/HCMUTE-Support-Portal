# 📊 BÁO CÁO PHÂN TÍCH KỸ THUẬT: CẢI TIẾN GIAO DIỆN FAQ, XỬ LÝ LỌC TRÙNG, TÌM KIẾM KHÔNG RELOAD & TINH GỌN DANH MỤC
**Ngày báo cáo:** 23-10-2026  
**Tác giả:** Dev A (Backend Lead & Database Architect) & Dev B (Frontend & UI/UX Specialist)  
**Mã dự án:** HCMUTE-Support-Portal (QAUTE Portal)  
**Nhánh:** `feature/ui-forum-faq`

---

## 📌 1. TỔNG QUAN 4 VẤN ĐỀ GIAO DIỆN & TRẢI NGHIỆM NGƯỜI DÙNG (UX)

Người dùng phản ánh 4 điểm bất cập trong quá trình sử dụng phân hệ FAQ:
1. **Vấn đề 1 (Lọc phòng ban bị trùng lặp):** Trong dropdown `#dept-filter`, các đơn vị (như "Khoa Công nghệ Thông tin", "Khoa Ngoại ngữ", "Phòng Đào tạo"...) xuất hiện 2 lần trong cùng một danh sách chọn.
2. **Vấn đề 2 (Khó chịu do Reload trang khi tìm kiếm):** Mỗi lần người dùng nhấn nút "Tìm kiếm" hoặc nhấn phím Enter trên ô tìm kiếm, trình duyệt thực hiện request `GET /faqs?q=...` và tải lại toàn bộ trang web (Full Page Reload), làm gián đoạn trải nghiệm người dùng.
3. **Vấn đề 3 (Tìm kiếm theo thời gian thực chưa linh động):** Người dùng muốn khi gõ bất kỳ ký tự nào vào ô tìm kiếm thì danh sách câu hỏi và các danh mục bên dưới phải tự động chạy theo từ khóa đó ngay lập tức (Real-time Live Reactive Search).
4. **Vấn đề 4 (Danh mục rác không có dữ liệu):** Hai danh mục "Tài khoản & hệ thống" và "Dịch vụ sinh viên" đều có số lượng câu hỏi là 0 trong bộ tri thức thực tế 351 câu hỏi, gây thừa thãi và mất cân đối thị giác.

---

## 🔬 2. NGUYÊN NHÂN KỸ THUẬT CỐT LÕI

### 2.1. Vấn đề 1: Trùng lặp thẻ Option Phòng ban
- Trong cả hai file template `faqs.html` và `faq/search.html`:
  ```html
  <select id="dept-filter">
      <option value="all">Tất cả đơn vị</option>
      <!-- Vòng lặp động nạp từ Database -->
      <option th:each="dept : ${departments}" th:value="${'dept-' + dept.id}" th:text="${dept.name}">Phòng Đào tạo</option>
      <!-- Khối tĩnh hardcode thừa bị sót lại từ bản mẫu cũ -->
      <option value="p-daotao">Phòng Đào tạo</option>
      <option value="p-ctsv">Phòng CTSV & Tuyển sinh</option>
      <option value="p-taichinh">Phòng Kế hoạch - Tài chính</option>
      <option value="k-cntt">Khoa Công nghệ Thông tin</option>
      <option value="k-nn">Khoa Ngoại ngữ</option>
      <option value="tt-cntt">Trung tâm CNTT</option>
  </select>
  ```
- Việc vừa có vòng lặp động từ Model `${departments}` vừa có các thẻ `<option>` hardcoded đã dẫn tới tình trạng trùng lặp danh sách và sai lệch giá trị `value` (`p-daotao` so với `dept-3`).

### 2.2. Vấn đề 2: Form Submit mặc định gây Reload trang
- Ô tìm kiếm nằm trong thẻ `<form th:action="@{/faqs}" method="get" id="faq-search-form">` với nút bấm mang thuộc tính `type="submit"`.
- Trình duyệt theo chuẩn HTML5 khi nhận phím Enter hoặc nút Submit sẽ lập tức trigger submit form, gửi HTTP GET lên máy chủ và tải lại toàn bộ trang.

### 2.3. Vấn đề 3 & 4: Danh mục rác và thiếu cơ chế cập nhật số đếm động
- Các pill "Tài khoản & hệ thống", "Dịch vụ sinh viên" được viết cứng trên giao diện mặc dù trong 351 câu hỏi không có câu nào thuộc nhóm này.
- Thuật toán client trước đó chưa tính toán lại số đếm trên các category pills theo thời gian thực khi người dùng gõ từ khóa tìm kiếm.

---

## 💡 3. GIẢI PHÁP & KIẾN TRÚC ĐÃ THỰC HIỆN

### 3.1. Dọn sạch Dropdown Phòng ban ([faqs.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/faqs.html) & [search.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/faq/search.html))
- Loại bỏ triệt để 6 thẻ `<option>` hardcode tĩnh.
- Chỉ giữ lại duy nhất:
  - `<option value="all">Tất cả đơn vị</option>`
  - `<option th:each="dept : ${departments}" th:value="${'dept-' + dept.id}" th:text="${dept.name}">Phòng Đào tạo</option>`
- Đảm bảo 100% dữ liệu hiển thị đồng nhất với bảng `departments` trong MySQL, không còn bị lặp.

### 3.2. Chuyển đổi sang kiến trúc Single-Page Reactive Search (Không Reload)
- Đổi nút `#search-submit-btn` từ `type="submit"` thành `type="button"`.
- Bổ sung Event Listener chặn sự kiện Submit trên `#faq-search-form`:
  ```javascript
  searchForm.addEventListener('submit', (e) => {
      e.preventDefault(); // Triệt tiêu hành vi reload trang
      searchQuery = searchInput.value.trim().toLowerCase();
      currentPage = 1;
      applyFilters();
  });
  ```

### 3.3. Tìm kiếm Live Real-time & Cập nhật số đếm Category Pills tự động
- Khi người dùng gõ từng ký tự vào ô `#faq-search-input` (sự kiện `input`):
  - Lập tức cập nhật `searchQuery`.
  - Hàm `updateCategoryPillCounts(faqItems)` tính toán lại số lượng câu hỏi phù hợp cho từng danh mục:
    - `#count-all`: Tổng số kết quả khớp từ khóa.
    - `#count-hoc-vu`, `#count-hoc-phi`, `#count-quy-che`, `#count-ngoai-ngu`: Số lượng câu hỏi thuộc từng danh mục khớp với từ khóa người dùng đang gõ.
  - Toolbar counter `#faq-counter-badge` cập nhật số đếm tức thì: `15 KẾT QUẢ` (thay vì tĩnh).
  - Phân trang tính toán lại số trang theo số kết quả thực tế và cuộn mượt mà.

### 3.4. Tinh gọn Danh mục Hỗ trợ
- Đã loại bỏ hoàn toàn 2 nút danh mục không có dữ liệu:
  - ❌ Bỏ `tai-khoan` ("Tài khoản & hệ thống")
  - ❌ Bỏ `dich-vu` ("Dịch vụ sinh viên")
- Giữ lại bộ 4 danh mục chuẩn hóa bám sát hoạt động học vụ HCMUTE:
  1. **Tất cả** (`all`)
  2. **Học vụ & đào tạo** (`hoc-vu`)
  3. **Học phí & tài chính** (`hoc-phi`)
  4. **Quy chế & tốt nghiệp** (`quy-che`)
  5. **Ngoại ngữ & chuẩn đầu ra** (`ngoai-ngu`)

---

## 🧪 4. KẾT QUẢ KIỂM THỬ THỰC TẾ

| Kịch bản kiểm thử | Hành vi kỳ vọng | Kết quả thực tế | Đánh giá |
|:---|:---|:---|:---:|
| Mở dropdown "Đơn vị phòng ban" | Mỗi phòng ban chỉ xuất hiện duy nhất 1 lần | Không còn bị nhân đôi Khoa CNTT, Ngoại ngữ, Đào tạo | Đạt |
| Nhập từ khóa (VD: "toeic") vào ô tìm kiếm | Danh sách câu hỏi bên dưới tự động lọc theo thời gian thực (0ms lag), không reload trang | Lọc tức thì, hiển thị đúng các câu hỏi về TOEIC | Đạt |
| Quan sát các nút Danh mục khi gõ từ khóa | Số lượng trên từng pill (`#count-hoc-phi`, `#count-ngoai-ngu`...) thay đổi theo từ khóa | Các con số trên pill nhảy số linh động theo kết quả lọc | Đạt |
| Nhấn phím Enter hoặc nút "Tìm kiếm" | Không tải lại trang, giữ nguyên vị trí cuộn | Trang web không reload, kết quả hiển thị mượt mà | Đạt |
| Kiểm tra các nút Danh mục hỗ trợ | Không còn thấy mục "Tài khoản" và "Dịch vụ sinh viên" | Giao diện gọn gàng với 5 danh mục chuẩn xác | Đạt |

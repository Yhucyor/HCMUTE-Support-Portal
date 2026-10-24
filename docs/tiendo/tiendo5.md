# 📋 TIẾN ĐỘ & PHƯƠNG ÁN CẢI TIẾN MODULE FAQ (TIENDO 5)
**Ngày thực hiện:** 22-10-2026  
**Nhánh Git:** `feature/ui-forum-faq`  
**Mục tiêu chính:**
1. Bổ sung 2 câu hỏi - trả lời thực tế vào bộ dữ liệu FAQ (Dataset 350+ câu) & đồng bộ Database.
2. Khắc phục dứt điểm lỗi nhấp vào câu hỏi không hiển thị câu trả lời (Accordion toggle collision).
3. Ẩn hoàn toàn khối "Cam kết thời hạn xử lý (SLA)" đối với giao diện Sinh viên và Khách vãng lai (chỉ mở cho Staff/Admin/Head).
4. Khắc phục triệt để tình trạng danh sách câu hỏi in tràn màn hình (351 câu in liên tục không phân trang) bằng giải pháp Phân trang Client-side mượt mà (10 câu/trang).

---

## 🎯 1. BẢNG PHÂN TÍCH VẤN ĐỀ & PHƯƠNG ÁN GIẢI QUYẾT

| STT | Vấn đề ghi nhận | Nguyên nhân kỹ thuật | Phương án giải quyết | Trạng thái |
|:---:|:---|:---|:---|:---:|
| 1 | Bổ sung 2 câu hỏi thực tế (AVĐR tháng 11 & Môn Toán 2 tương đương Giải tích) | Bộ câu hỏi thực tế mới phát sinh từ sinh viên HCMUTE | Cập nhật file `docs/dataset/faq_dataset_curated.json` (ID 136544, 136545) & tự động đồng bộ vào DB qua `DataInitializer.syncFaqsFromDataset()` | ✅ Hoàn thành |
| 2 | Nhấp vào câu hỏi nhưng không xổ câu trả lời | Xung đột sự kiện (Event Listener Collision): File template có cả script nội tuyến `initFAQApp` và script `faq-accordion.js` cùng bắt click `.faq-toggle`, khiến class `hidden` bị đảo ngược 2 lần ngay lập tức | Loại bỏ toàn bộ khối script nội tuyến thừa trong `faqs.html` và `faq/search.html`; chuyển toàn bộ quyền kiểm soát cho `faq-accordion.js` với kỹ thuật Event Delegation | ✅ Hoàn thành |
| 3 | Cam kết thời hạn xử lý (SLA) hiển thị cho Sinh viên | Thẻ SLA Sidebar trong giao diện không có điều kiện lọc quyền Spring Security | Thêm thuộc tính `sec:authorize="hasAnyRole('STAFF', 'ADMIN', 'DEPARTMENT_HEAD')"` vào thẻ SLA Card ở cả 2 template `faqs.html` và `faq/search.html` | ✅ Hoàn thành |
| 4 | Danh sách FAQ in liên tục 351 câu làm tràn trang | Không có cơ chế phân trang Client-side khi render danh sách FAQ từ Model Thymeleaf | Tích hợp thuật toán Phân trang động (10 câu/trang) trong `faq-accordion.js`, tự động tính số trang, sinh nút Previous/Next/Số trang, và smooth scroll | ✅ Hoàn thành |
| 5 | Ngày tháng năm hỏi bị lẫn lộn giữa các năm | Truy vấn `findAllActiveWithDepartment` thiếu `ORDER BY`, Client-side chưa sort theo timestamp | Thêm `ORDER BY COALESCE(f.postDate, f.createdAt) DESC, f.viewCount DESC, f.id DESC` ở Repository & bổ sung `timestamp` vào `FaqItemDto`, tự động sort ưu tiên năm 2026, 2025 lên đầu trang | ✅ Hoàn thành |
| 6 | Phòng ban / khoa trong bộ lọc filter bị lặp lại 2 lần | Template vừa có vòng lặp động `${departments}` vừa có các thẻ `<option>` tĩnh hardcoded | Xóa bỏ toàn bộ các thẻ tĩnh thừa, chỉ giữ lại vòng lặp động chuẩn từ database MySQL | ✅ Hoàn thành |
| 7 | Bấm tìm kiếm bị reload toàn bộ trang web | Nút bấm mang `type="submit"` và form submit mặc định kích hoạt full-page refresh | Chuyển nút bấm sang `type="button"`, chặn submit form với `e.preventDefault()`, lọc Single-Page tức thời | ✅ Hoàn thành |
| 8 | Tìm kiếm chưa linh động & 2 danh mục có 0 câu hỏi | Input chưa đồng bộ số đếm các category pills; mục "Tài khoản" và "Dịch vụ" có 0 câu | Loại bỏ 2 danh mục rác; cập nhật hàm `updateCategoryPillCounts` trong `faq-accordion.js` để badge danh mục nhảy số real-time khi gõ phím | ✅ Hoàn thành |

---

## 🛠️ 2. CHI TIẾT CÁC TỆP TIN ĐÃ THAY ĐỔI

### 2.1. Cập nhật Dataset: `docs/dataset/faq_dataset_curated.json`
- Đã thêm 2 mục FAQ mới:
  1. `136544`: **"Kiến nghị xem xét mở bổ sung đợt thi AVĐR tháng 11/2026 để xét tốt nghiệp tháng 12/2026"**  
     - Phân loại: Chuẩn đầu ra ngoại ngữ (`ngoai-ngu`)
     - Trả lời: Nhà trường chưa có kế hoạch mở thi tháng 11; hướng dẫn sinh viên thi chứng chỉ quốc tế được công nhận bên ngoài.
  2. `136545`: **"Phương án xử lý học phần học lại Toán 2 đối với sinh viên khóa cũ khi chương trình đào tạo chuyển sang học phần Giải tích"**  
     - Phân loại: Quy chế đào tạo (`quy-che`)
     - Trả lời: Trong kỳ vẫn mở học phần Toán 2; nếu không mở nữa thì theo dõi văn bản tương đương hoặc liên hệ Bộ môn Toán / Khoa KHƯD.
- Tổng số lượng FAQ tăng lên **351 câu hỏi**.

### 2.2. Cơ chế Tự động Đồng bộ DB: `src/main/java/com/school/counseling/config/DataInitializer.java`
- Bổ sung logic `syncFaqsFromDataset()`: So sánh danh sách câu hỏi trong `faq_dataset_curated.json` với bảng `faqs` theo tiêu đề câu hỏi (`question.trim()`).
- Bất kỳ câu hỏi nào có trong file JSON mà Database chưa có sẽ được nạp tự động ngay khi khởi động ứng dụng mà không cần drop database.

### 2.3. Module JavaScript Quản lý Phân trang & Accordion: `src/main/resources/static/js/modules/faq-accordion.js`
- **Phân trang (Pagination):** Thiết lập `pageSize = 10`. Tự động tính toán số trang dựa trên kết quả lọc thực tế (`matchingItems`).
- **Nút bấm điều hướng:** Tự động render nút Previous, Next, các trang số (với dấu `...` khi tổng số trang lớn).
- **Accordion:** Lắng nghe click thông qua Event Delegation trên `#faq-list`, kiểm tra trạng thái hiển thị của `.faq-content` và xoay mũi tên `.faq-chevron`.
- **Bộ lọc kết hợp:** Tương thích hoàn toàn với bộ lọc danh mục (`data-cat`), phòng ban (`data-dept`), tab (`data-type`), và ô tìm kiếm Live Search.

### 2.4. Template Giao diện: `templates/faqs.html` & `templates/faq/search.html`
- Thêm container phân trang `#faq-pagination-container` ngay dưới `#faq-empty-state`.
- Đặt `sec:authorize="hasAnyRole('STAFF', 'ADMIN', 'DEPARTMENT_HEAD')"` bao bọc Card cam kết SLA để bảo mật nghiệp vụ nội bộ.
- Xóa bỏ khối script inline dư thừa trùng lặp để loại trừ race condition.

---

## 🧪 3. QUY TRÌNH KIỂM CHỨNG & KẾT QUẢ TEST
1. **Kiểm tra Biên dịch:** `mvn test-compile` hoàn toàn thành công, không phát sinh lỗi cú pháp hay thiếu thư viện.
2. **Kiểm tra API / HTML:**
   - Truy cập `GET http://localhost:8080/faqs`:
     - Trang hiển thị đúng 10 câu hỏi đầu tiên.
     - Thanh phân trang hiển thị `Đang hiển thị 1 - 10 trong tổng số 351 câu hỏi` cùng các nút trang 1, 2, 3... 36, Next.
     - Card SLA không xuất hiện đối với tài khoản Sinh viên / Khách vãng lai.
   - Nhấp vào câu hỏi: Câu trả lời mở ra mượt mà, mũi tên xoay 180 độ; nhấp lại đóng lại chính xác.

---

## 🎯 4. GIAI ĐOẠN 4 (23-10-2026): TỐI ƯU THANH TÌM KIẾM & PHÍM TẮT ENTER
- **Loại bỏ viền xanh vuông góc bên trong:** Triệt tiêu hoàn toàn outline mặc định trên `<input id="faq-search-input">` (`border-0 outline-none focus:outline-none focus:ring-0 focus:border-0 shadow-none` và inline style `outline: none !important; border: none !important; box-shadow: none !important;`).
- **Thay thế nút Tìm kiếm:** Thay đổi nút xanh lớn thành nút biểu tượng phím `↵ Enter` gọn nhẹ, hiện đại.
- **Bắt phím Enter trên máy tính:** Lắng nghe sự kiện `keydown` phím `Enter` trên ô input, ngăn tải lại trang, kích hoạt tìm kiếm và tự động cuộn mượt xuống danh sách câu hỏi (`scrollToFaqList()`) với khoảng đệm trừ hao 90px chiều cao navbar.

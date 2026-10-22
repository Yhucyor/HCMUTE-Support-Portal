# 📊 BÁO CÁO PHÂN TÍCH KỸ THUẬT & KIẾN TRÚC: MODULE FAQ, SLA CARD & PHÂN TRANG
**Ngày báo cáo:** 22-10-2026  
**Tác giả:** Dev A (Backend Lead & Database Architect) & Dev B (Frontend & UI/UX Specialist)  
**Mã dự án:** HCMUTE-Support-Portal (QAUTE Portal)  
**Nhánh:** `feature/ui-forum-faq`

---

## 📌 1. TỔNG QUAN YÊU CẦU & BỐI CẢNH
Người dùng đưa ra 4 yêu cầu cấp thiết về dữ liệu và trải nghiệm người dùng (UX) trên phân hệ Hỏi Đáp Thường Gặp (FAQ):
1. **Bổ sung 2 câu hỏi - trả lời thực tế từ sinh viên HCMUTE** vào bộ cơ sở dữ liệu tri thức 300+ FAQ:
   - *Câu 1:* Kiến nghị mở thêm đợt thi AVĐR tháng 11/2026 để kịp xét tốt nghiệp tháng 12/2026.
   - *Câu 2:* Phương án xử lý học phần Toán 2 nợ đối với sinh viên khóa cũ khi CTĐT đổi sang Giải tích.
2. **Khắc phục lỗi Click vào câu hỏi không hiển thị câu trả lời:** Giao diện bị đơ/không phản hồi khi người dùng nhấp vào tiêu đề câu hỏi.
3. **Ẩn hoàn toàn bảng Cam kết thời hạn xử lý (SLA) đối với giao diện Sinh viên:**
   - Các mức cam kết: URGENT (24h), HIGH (48h), MEDIUM (72h), LOW (7d) là quy định vận hành và kiểm soát hiệu năng nội bộ của các Phòng/Ban và Cán bộ chuyên trách. Sinh viên chỉ cần gửi yêu cầu và nhận kết quả, không cần hiển thị bảng này làm rườm rà giao diện tra cứu tri thức.
4. **Giải quyết vấn đề in liên tục 351 câu hỏi làm tràn màn hình (Overflow UX):**
   - Danh sách FAQ dài hàng trăm mục làm người dùng phải cuộn chuột rất nhiều trang, giảm hiệu năng render DOM và phá vỡ cấu trúc thẩm mỹ của giao diện. Yêu cầu phân trang hiển thị hợp lý (10 câu/trang).

---

## 🔬 2. NGUYÊN NHÂN SÂU XA (ROOT CAUSE ANALYSIS)

### 2.1. Lỗi Accordion Click Không Xổ Câu Trả Lời (Event Collision)
- **Hiện tượng:** Người dùng click vào thẻ câu hỏi `.faq-toggle`, câu trả lời không mở ra hoặc chớp nhoáng rồi biến mất.
- **Nguyên nhân cốt lõi:**
  - Trong quá trình phát triển, file template `faqs.html` và `faq/search.html` có chứa một đoạn script nội tuyến:
    ```javascript
    faqItems.forEach(item => {
        const toggle = item.querySelector('.faq-toggle');
        const content = item.querySelector('.faq-content');
        toggle.addEventListener('click', () => {
            content.classList.toggle('hidden'); // Listener 1
        });
    });
    ```
  - Đồng thời, ở cuối trang lại nạp file module tách rời `faq-accordion.js` cũng chứa Event Delegation:
    ```javascript
    faqList.addEventListener('click', (e) => {
        const toggleBtn = e.target.closest('.faq-toggle');
        // ...
        content.classList.toggle('hidden'); // Listener 2
    });
    ```
  - Khi người dùng click chuột 1 lần vào `.faq-toggle`, cả hai Listener đều lập tức được kích hoạt tuần tự:
    $$\text{Hidden} \xrightarrow{\text{Listener 1}} \text{Visible} \xrightarrow{\text{Listener 2}} \text{Hidden}$$
  - Kết quả là class `hidden` bị xóa rồi lại thêm vào ngay trong cùng một frame rendering, người dùng không thể thấy nội dung câu trả lời.

### 2.2. Vấn đề Hiển thị Thẻ SLA (SLA Card Exposure)
- Khối HTML của `SLA Commitment Card` trong Sidebar được code tĩnh không có bộ lọc phân quyền `sec:authorize`.
- Bất kỳ ai truy cập vào `/faqs` hoặc `/faqs/search` (bao gồm sinh viên chính quy, khách chưa đăng nhập) đều nhìn thấy các chỉ số SLA nội bộ.

### 2.3. Vấn đề Tràn Giao diện do Thiếu Phân Trang (DOM Bloat & Infinite Scroll Glitch)
- Khi nạp toàn bộ 349-351 bản ghi FAQ từ MySQL ORM ra giao diện bằng vòng lặp `th:each="faq : ${faqs}"`, trình duyệt phải khởi tạo 351 DOM Nodes phức tạp chứa icon, badge, nút đánh giá hữu ích, chevron...
- Toàn bộ 351 phần tử đều có trạng thái hiển thị `block` mặc định, kéo dài trang web hàng ngàn pixels.

---

## 💡 3. KIẾN TRÚC & GIẢI PHÁP KỸ THUẬT ĐÃ THỰC HIỆN

### 3.1. Dữ liệu & Đồng bộ Database Tự động (Dev A)
1. **Dataset Curated (`docs/dataset/faq_dataset_curated.json`):**
   - Đã thêm 2 mục FAQ với mã định danh duy nhất:
     - `136544`: Danh mục `ngoai-ngu`, Thẻ: `['tieng-anh', 'avdr', 'chuan-dau-ra', 'tot-nghiep']`.
     - `136545`: Danh mục `quy-che`, Thẻ: `['toan-2', 'giai-tich', 'hoc-lai', 'mon-tuong-duong']`.
   - Nâng tổng số lượng FAQ được chuẩn hóa lên **351 câu hỏi**.
2. **Cơ chế Auto-Sync Database (`DataInitializer.java`):**
   - Thay vì chỉ kiểm tra `faqRepository.count() == 0` (chỉ nạp lần đầu khi DB rỗng), phương thức `syncFaqsFromDataset()` được cài đặt:
     ```java
     Set<String> existingQuestions = faqRepository.findAll().stream()
             .map(f -> f.getQuestion().trim().toLowerCase())
             .collect(Collectors.toSet());
     // Chỉ chèn những câu hỏi trong dataset mà DB chưa có
     ```
   - Nhờ vậy, mỗi khi bổ sung câu hỏi mới vào JSON, hệ thống sẽ tự động cập nhật vào cơ sở dữ liệu MySQL mà không làm mất dữ liệu đã có hay cần drop bảng.

### 3.2. Phân Trang Client-Side & Khắc Phục Accordion (Dev B)
1. **Loại bỏ triệt để xung đột mã nguồn:**
   - Đã xóa sạch khối script `(function initFAQApp() { ... })()` nội tuyến trong cả hai tệp `templates/faqs.html` và `templates/faq/search.html`.
   - Toàn bộ luồng nghiệp vụ giao diện được tập trung vào duy nhất file `src/main/resources/static/js/modules/faq-accordion.js`.
2. **Thuật toán Phân trang Client-Side (10 câu/trang):**
   - Đặt `pageSize = 10`.
   - Lọc danh sách thỏa mãn đồng thời:
     $$\text{Item hợp lệ} = \text{Match(Category)} \land \text{Match(Dept)} \land \text{Match(Tab)} \land \text{Match(SearchQuery)}$$
   - Thuật toán hiển thị theo khoảng chỉ mục:
     $$\text{Display} = \begin{cases} \text{block}, & \text{nếu } (Page - 1) \times 10 \le \text{Index} < Page \times 10 \\ \text{none}, & \text{ngược lại} \end{cases}$$
   - Thanh điều hướng phân trang tự động hiển thị:
     - Nút "Trang trước" (disable nếu đang ở trang 1).
     - Các nút số trang thông minh: trang đầu, trang cuối, các trang lân cận trang hiện tại, và dấu `...` rút gọn.
     - Nút "Trang sau" (disable nếu đang ở trang cuối).
     - Khi chuyển trang, giao diện tự động cuộn nhẹ (`scrollIntoView({ behavior: 'smooth' })`) lên đầu danh sách câu hỏi.
3. **Event Delegation cho Accordion:**
   - Lắng nghe duy nhất trên thẻ cha `#faq-list`:
     ```javascript
     const toggleBtn = e.target.closest('.faq-toggle');
     if (toggleBtn) {
         const item = toggleBtn.closest('.faq-item');
         const content = item.querySelector('.faq-content');
         const chevron = item.querySelector('.faq-chevron');
         const isHidden = content.classList.contains('hidden');
         content.classList.toggle('hidden', !isHidden);
         if (chevron) chevron.classList.toggle('rotate-180', isHidden);
     }
     ```
   - Đảm bảo mỗi lần click chỉ thực thi duy nhất 1 thao tác đảo trạng thái, mở/đóng chính xác 100%.

### 3.3. Bảo Mật & Tối Ưu Giao Diện SLA Card
- Áp dụng phân quyền trực tiếp trên Thymeleaf View:
  ```html
  <div sec:authorize="hasAnyRole('STAFF', 'ADMIN', 'DEPARTMENT_HEAD')" 
       class="rounded-2xl bg-white border border-[#e2e8f0] p-5 shadow-sm space-y-3">
      <!-- Nội dung SLA URGENT, HIGH, MEDIUM, LOW -->
  </div>
  ```
- Kết quả:
  - **Sinh viên / Khách vãng lai:** Card SLA hoàn toàn không render trong DOM (bảo mật tuyệt đối, giao diện tinh gọn, tập trung tra cứu).
  - **Cán bộ / Trưởng phòng ban / Ban Giám Hiệu:** Vẫn nhìn thấy bảng cam kết SLA để đối chiếu KPI phục vụ.

---

## 📊 4. KẾT QUẢ KIỂM THỬ THỰC TẾ

| Kịch bản kiểm thử | Kỳ vọng | Kết quả thực tế | Đánh giá |
|:---|:---|:---|:---:|
| Khởi động ứng dụng Spring Boot | DB tự động nạp thêm 2 câu hỏi mới nếu chưa có | Bảng `faqs` tăng lên 351 bản ghi | Đạt |
| Truy cập `/faqs` với quyền Khách / Sinh viên | Danh sách chỉ hiện 10 câu đầu, có thanh phân trang 1..36 | Hiển thị 1 - 10 trong tổng số 351 câu hỏi, có thanh điều hướng chuẩn | Đạt |
| Kiểm tra hiển thị Card SLA trên giao diện Sinh viên | Card cam kết thời hạn SLA không xuất hiện | Ẩn hoàn toàn khỏi DOM (không xuất hiện thẻ HTML) | Đạt |
| Nhấp chuột vào bất kỳ câu hỏi FAQ nào | Phần câu trả lời xổ xuống, icon mũi tên quay ngược | Câu trả lời hiển thị rõ ràng, mượt mà | Đạt |
| Nhấp lại vào câu hỏi đang mở | Câu trả lời thu gọn lại, icon mũi tên trở về vị trí cũ | Thu gọn thành công | Đạt |
| Chuyển sang trang 2, trang 3 trên thanh phân trang | Hiển thị đúng 10 câu tiếp theo, màn hình tự cuộn lên đầu danh sách | Chuyển trang mượt mà không load lại trang | Đạt |
| Nhập từ khóa tìm kiếm (VD: "Toán 2", "AVĐR") | Tự động reset về trang 1 và hiển thị đúng các câu hỏi liên quan | Bộ lọc hoạt động tức thời, tìm thấy đúng 2 câu hỏi mới thêm | Đạt |

---

## 🚀 5. KẾT LUẬN & ĐỀ XUẤT TIẾP THEO
- Phân hệ FAQ đã đạt độ hoàn thiện cao về cả dữ liệu (351 câu hỏi chính xác thực tế HCMUTE), bảo mật (ẩn SLA nội bộ) và trải nghiệm tương tác mượt mà (Accordion + Phân trang 10 câu/trang).
- Kế hoạch tiếp theo:
  - Tiếp tục kiểm tra các trường hợp ngoại lệ trên môi trường di động (Mobile Responsive).
  - Chuẩn bị đồng bộ các câu hỏi mới vào Vector Store của RAG Chatbot để AI trả lời được ngay các câu hỏi về AVĐR và Toán 2.

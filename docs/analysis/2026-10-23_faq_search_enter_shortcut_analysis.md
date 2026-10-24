# BÁO CÁO KỸ THUẬT: TỐI ƯU GIAO DIỆN THANH TÌM KIẾM FAQ & THAY THẾ PHÍM TẮT ENTER

- **Ngày thực hiện:** 23-10-2026
- **Module:** Tra cứu FAQ (`FaqWebController`, `faqs.html`, `faq/search.html`, `faq-accordion.js`)
- **Tác giả:** Dev A (Backend Lead & Fullstack Engineer)
- **Mục tiêu:**
  1. Loại bỏ viền xanh vuông góc bên trong ô tìm kiếm khi focus (inner focus border).
  2. Thay thế nút "Tìm kiếm" to màu xanh bằng biểu tượng phím `↵ Enter` gọn gàng, tinh tế.
  3. Bắt sự kiện phím `Enter` trên máy tính để tìm kiếm và cuộn mượt (`scrollIntoView` có tính offset header) xuống danh sách câu hỏi.

---

## 1. PHÂN TÍCH VẤN ĐỀ & NGUYÊN NHÂN

### 1.1. Viền xanh bên trong thanh tìm kiếm (Inner Blue Border)
- **Hiện tượng:** Khi người dùng click hoặc focus vào ô `<input>` tìm kiếm, trình duyệt (Chrome, Edge...) áp dụng outline mặc định (`outline: -webkit-focus-ring-color auto 1px`) hoặc thuộc tính viền focus mặc định, tạo ra một hình chữ nhật viền xanh với góc vuông nằm lọt thỏm bên trong thanh tìm kiếm bo tròn (`rounded-xl`).
- **Khắc phục:** 
  - Khai báo class Tailwind: `border-0 outline-none focus:outline-none focus:ring-0 focus:border-0 shadow-none`.
  - Khai báo inline style cưỡng chế: `style="outline: none !important; border: none !important; box-shadow: none !important;"`.
  - Giữ lại hiệu ứng focus phát sáng nhẹ nhàng ở container bọc ngoài (`focus-within:ring-2 focus-within:ring-[#0037b0]/30`).

### 1.2. Nút tìm kiếm & Trải nghiệm phím Enter
- **Hiện tượng:** Nút `[ Tìm kiếm ]` màu xanh nền đậm chiếm diện tích lớn ở góc phải thanh tìm kiếm, trong khi hầu hết người dùng desktop thường bấm phím `Enter` trên bàn phím.
- **Khắc phục:**
  - Thay thế nút bấm bằng biểu tượng phím tắt `↵ Enter` thanh lịch (`bg-white hover:bg-[#eff4ff] text-[#0037b0] border border-[#dce1ff]`).
  - Lắng nghe sự kiện `keydown` trên `#faq-search-input`: Khi `e.key === 'Enter'`, chặn hành vi reload (`e.preventDefault()`), cập nhật `searchQuery`, chạy `applyFilters()`, và tự động cuộn màn hình (`scrollToFaqList()`) xuống kết quả tìm kiếm với offset trừ hao 90px để không bị header che khuất.

---

## 2. CÁC TỆP TIN ĐÃ THAY ĐỔI
1. `src/main/resources/templates/faqs.html`: Cập nhật input search và nút phím Enter.
2. `src/main/resources/templates/faq/search.html`: Cập nhật đồng bộ cấu trúc tương tự.
3. `src/main/resources/static/js/modules/faq-accordion.js`:
   - Bổ sung sự kiện `keydown` cho phím `Enter` trên input.
   - Cập nhật hàm `scrollToFaqList()` tính toán vị trí trừ đi 90px chiều cao navbar.
   - Kích hoạt `scrollToFaqList()` khi bấm Enter hoặc click nút `↵ Enter`.

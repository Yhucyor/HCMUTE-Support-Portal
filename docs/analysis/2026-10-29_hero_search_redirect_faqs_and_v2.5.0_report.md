# 📑 BÁO CÁO THAY ĐỔI: REDIRECT HERO SEARCH VÀ PHÁT HÀNH V2.5.0

**Ngày thực hiện:** 2026-10-29  
**Người thực hiện:** Dev A (Fullstack & Software Architect)  
**Phiên bản phát hành:** `v2.5.0` (Tags trên `main`, đồng bộ `dev`)  

---

## 🎯 1. YÊU CẦU THỰC HIỆN
- Chuyển hướng thanh tìm kiếm trung tâm (Hero Search) và các chip từ khóa gợi ý tại trang chủ (`index.html`) trực tiếp sang trang tra cứu FAQ (`/faqs?q=...`) thay vì kích hoạt mở modal Chatbot AI Mascot.
- Chuẩn hóa kiến trúc MVC, dọn dẹp các repository dead queries, sắp xếp lại thư mục Thymeleaf templates theo domain modules.

---

## 🛠️ 2. CHI TIẾT CÁC TẬP TIN VÀ DÒNG CODE ĐÃ THAY ĐỔI

### 1. [`src/main/resources/templates/index.html`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/index.html)
- **Vị trí dòng:** [L96 - L118](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/index.html#L96-L118)
  * **Trước:** Khối `div` bọc `input` và `button` bắt sự kiện `onclick="executeSearch()"`, các chip gợi ý gọi `quickSearch(...)`.
  * **Sau:** Chuyển sang form chuẩn HTML5 `GET` method trỏ tới `/faqs`:
    ```html
    <form action="/faqs" method="get" class="bg-white/95 backdrop-blur-md p-1.5 rounded-full shadow-2xl border border-white/80 flex items-center gap-2">
        <div class="flex items-center pl-3 text-slate-400">
            <span class="material-symbols-outlined text-[20px] text-[#003882]">search</span>
        </div>
        <input id="heroSearchInput" name="q" type="text" placeholder="Tìm kiếm thông tin tuyển sinh, ngành học, FAQ, quy chế học vụ..." 
               class="flex-1 bg-transparent border-none outline-none text-xs sm:text-sm text-slate-800 placeholder-slate-400 px-2 py-1.5 focus:ring-0">
        <button type="submit" class="inline-flex items-center gap-1.5 px-5 py-2.5 bg-[#003882] hover:bg-[#002456] text-white text-xs sm:text-sm font-semibold rounded-full shadow-md transition-all active:scale-95">
            <span>Tìm kiếm</span>
            <span class="material-symbols-outlined text-[16px]">arrow_forward</span>
        </button>
    </form>
    ```
- **Vị trí dòng:** [L849 - L857](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/index.html#L849-L857)
  * **Trước:** `toggleChatWindow(true); handleQuickClick(q);` (kích hoạt mở popup chatbot và gửi câu hỏi vào widget chat).
  * **Sau:** Thay bằng lệnh redirect trình duyệt:
    ```javascript
    function executeSearch() {
        const input = document.getElementById('heroSearchInput');
        const q = input ? input.value.trim() : '';
        if (q) {
            window.location.href = '/faqs?q=' + encodeURIComponent(q);
        } else {
            window.location.href = '/faqs';
        }
    }
    ```

### 2. [`src/main/java/com/school/counseling/module/ticket/service/TicketService.java`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/ticket/service/TicketService.java)
- **Vị trí dòng:** [L778](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/ticket/service/TicketService.java#L778)
  * Thêm comment ghi rõ cơ chế bảo mật chống IDOR và tối ưu hiệu năng thông qua `findAccessAuthInfoById`.

---

## 🔒 3. ĐÁNH GIÁ AN TOÀN BẢO MẬT
- **XSS Prevention:** Input tìm kiếm đi qua tham số query `q`, hiển thị lại trên view thông qua Thymeleaf `th:value` có cơ chế tự động HTML-escape.
- **URL Encoding:** Sử dụng hàm tiêu chuẩn `encodeURIComponent(q)` chống lỗi format URL và HTTP parameter pollution.
- **Spring Data MVC:** Tương thích trực tiếp với endpoint `@GetMapping({"/faqs", "/faq"})` trong `FaqWebController.java`.

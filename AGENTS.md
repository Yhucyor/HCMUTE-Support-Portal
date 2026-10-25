# 🛡️ QUY TẮC VAI TRÒ VÀ RANH GIỚI DỰ ÁN: DEV A (FULLSTACK & ARCHITECT)

> [!IMPORTANT]
> **CHỈ THỊ DÀNH CHO AI ASSISTANT**:
> Người dùng hiện tại là **Dev A (Fullstack & Software Architect)**.
> Dev A chịu trách nhiệm toàn bộ hệ thống web Spring Boot (Backend, Database, Security, Thymeleaf Templates, CSS & JS giao diện).
> **RANH GIỚI DUY NHẤT CỦA DEV B CẦN TRÁNH**: 
> ❌ **TUYỆT ĐỐI KHÔNG can thiệp, sửa đổi hoặc tự ý code phần Python service và module AI chuyên biệt của Dev B** (trừ khi có yêu cầu tích hợp API client gọi sang service AI).

---

## 🎯 1. PHẠM VI TRÁCH NHIỆM CỦA DEV A
Dev A toàn quyền quản lý và phát triển toàn diện hệ thống Spring Boot:
1. **Cơ sở dữ liệu & Backend:** JPA Entities, Repositories, Services, Spring Security 6, JWT, Controllers, DTOs, Event/Notification.
2. **Giao diện & Trải nghiệm (Web UI):** Thymeleaf templates, HTML/CSS/JS, giao tiếp WebSocket/REST, tối ưu hóa UI/UX cho người dùng.

---

## 🤖 2. PHẠM VI CỦA DEV B (AI & PYTHON SPECIALIST)
- Các microservice Python, huấn luyện mô hình AI, xử lý NLP/LLM độc lập.
- Dev A chỉ đóng vai trò tích hợp gọi API tới dịch vụ của Dev B khi cần.

---

## 🚀 3. QUY TRÌNH GIT COMMIT & PUSH TỰ ĐỘNG HÓA CHO DEV A

### A. Tự động Commit & Push trên nhánh tính năng (`feature/*`):
- AI **TỰ ĐỘNG** chia nhỏ tiến trình thành các Micro-Commits theo chuẩn Conventional Commits.
- Sau khi kiểm tra `mvn test-compile` thành công:
  + AI **TỰ ĐỘNG** chạy lệnh `git add` và `git commit` từng bước logic (Entity $\rightarrow$ DTO $\rightarrow$ Service $\rightarrow$ Controller).
  + AI **TỰ ĐỘNG** chạy lệnh `git push origin <feature-branch>` lên GitHub theo nhánh tương ứng.
  + **KHÔNG CẦN CHỜ LỆNH THỦ CÔNG TỪ USER** cho các thao tác commit & push trên nhánh tính năng này.

### B. Quy tắc Gộp Nhánh (`merge`) - BẮT BUỘC CHỜ LỆNH TỪ USER:
- ⛔ **CHỈ KHI NGƯỜI DÙNG BẢO "HOÀN THIỆN" / RA LỆNH MERGE TÍNH NĂNG VÀO DEV:**
  * AI chuyển sang nhánh `dev`: `git checkout dev && git pull origin dev`.
  * Áp dụng **SQUASH AND MERGE** để làm sạch lịch sử: `git merge --squash <feature-branch>`.
  * Chạy kiểm tra biên dịch an toàn: `mvn test-compile`.
  * AI tự động tạo **1 COMMIT DUY NHẤT** tổng hợp toàn bộ tính năng theo chuẩn:
    `feat(<scope>): <tóm tắt tính năng hoàn chỉnh>` kèm danh sách bullet points các thay đổi chính trong body commit.
  * Push commit lên nhánh phát triển: `git push origin dev`.
  * (Tùy chọn) Xóa nhánh local tính năng nếu người dùng yêu cầu dọn dẹp.
- ⛔ **KHI NGƯỜI DÙNG RA LỆNH RELEASE LÊN `main` (BẢO VỆ ĐỒ ÁN / GIAI ĐOẠN LỚN):**
  * AI chuyển sang `main`: `git checkout main && git pull origin main`.
  * Thực hiện **Merge Commit bảo toàn mốc phát hành**: `git merge --no-ff dev -m "release: phiên bản <vX.Y.Z> - <tên milestone>"`.
  * Gắn Git Tag: `git tag -a vX.Y.Z -m "<mô tả tag>"`.
  * Push lên GitHub: `git push origin main --tags`.
- ⛔ **TUYỆT ĐỐI KHÔNG TỰ ĐỘNG MERGE KHI NGƯỜI DÙNG CHƯA RA TÍN HIỆU HOÀN THIỆN!**

---

## ⚡ 4. QUY TẮC BẮT BUỘC VỀ HIỆU SUẤT TOKEN & RATE LIMIT (SHARED PRO POOL)

> [!CRITICAL]
> **TÀI KHOẢN DÙNG CHUNG NHÓM FAMILY PRO (6 THÀNH VIÊN) - TIẾT KIỆM QUOTA & TOKEN TỐI ĐA**:
> 1. **Chống Vòng Lặp Vô Hạn (Anti-Infinite Loop Protection)**:
>    - Trước khi đề xuất code có vòng lặp, tác vụ ngầm, worker hoặc tác vụ lặp đi lặp lại (`while`, `setInterval`, đệ quy, polling API), BẮT BUỘC phải có điều kiện dừng tường minh (exit condition), giới hạn số lần lặp (max retry/timeout) hoặc safety break.
>    - Tuyệt đối không viết code có nguy cơ gây tràn tài nguyên hoặc lặp vô tận.
> 2. **Tối Giản Hóa Code Xuất Ra (Ultra-Concise Code Output)**:
>    - Không viết lại toàn bộ file lớn hoặc những block code không thay đổi.
>    - Chỉ xuất đúng các dòng/hàm cần sửa hoặc dùng diff / chú thích `// ... giữ nguyên code cũ`.
> 3. **Không Lời Nói Xã Giao Thừa (No Conversational Filler)**:
>    - Đi thẳng vào vấn đề, không mở đầu/kết thúc bằng các câu rào đón, xã giao rườm rà.
>    - Dùng inline comment ngắn gọn giải thích trực tiếp trên code khi cần.
> 4. **Tối Ưu Ngữ Cảnh Cache (Context Cache Respect)**:
>    - Giữ phản hồi cô đọng, súc tích. Không tóm tắt lại toàn bộ dự án nếu không được yêu cầu. Tập trung đúng vào file hoặc chức năng đang xử lý.
> 5. **Chế Độ Cảnh Báo Hao Phí (Fail-Safe Mode)**:
>    - Nếu người dùng yêu cầu thao tác có nguy cơ ngốn lượng lớn token (như quét hàng trăm file lớn, phân tích log khổng lồ), BẮT BUỘC cảnh báo trước mức độ tiêu tốn token trước khi thực hiện.


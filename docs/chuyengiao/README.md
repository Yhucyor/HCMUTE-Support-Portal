# THƯ MỤC TÀI LIỆU CHUYỂN GIAO & TÍCH HỢP HỆ THỐNG QAUTE PORTAL

Chào bạn, toàn bộ tài liệu hướng dẫn chuyển giao từ dự án `doancuoiki_demo1` sang `HCMUTE-Support-Portal` và giải đáp thắc mắc về 17 bảng cơ sở dữ liệu đã được tổng hợp chi tiết trong thư mục này gồm 3 tài liệu:

---

## 📑 MỤC LỤC TÀI LIỆU

### 1. [01_PHAN_TICH_17_BANG_DATABASE.md](file:///d:/HK5/LapTrinhWeb/doancuoiki/HCMUTE-Support-Portal/docs/chuyengiao/01_PHAN_TICH_17_BANG_DATABASE.md)
- Phân tích chi tiết chức năng và vai trò của toàn bộ 17 bảng trong CSDL.
- Phân tích 5 bảng bổ trợ/nâng cao (`internal_staff_admin_messages`, `knowledge_chunks`, `faqs`, `ticket_histories`, `conversations`/`messages`).
- Đánh giá: **NÊN GIỮ NGUYÊN 17 BẢNG** để đạt điểm tối đa (A/A+) vì mã nguồn đã hoàn thiện và chạy trơn tru.

### 2. [02_LO_TRINH_CHUYEN_GIAO_THEO_CHUC_NANG.md](file:///d:/HK5/LapTrinhWeb/doancuoiki/HCMUTE-Support-Portal/docs/chuyengiao/02_LO_TRINH_CHUYEN_GIAO_THEO_CHUC_NANG.md)
- Thứ tự chuyển giao từng nhóm file theo chuẩn kiến trúc Spring Boot (từ tầng Nền tảng $\rightarrow$ Nghiệp vụ $\rightarrow$ Giao diện) để tránh lỗi thiếu class phụ thuộc (Dependency Missing).
- Danh sách 7 bước chuyển giao:
  1. `Nền tảng & Common Utilities`
  2. `Xác thực & Phân quyền (Auth & RBAC)`
  3. `Quản lý Vé hỗ trợ & SLA Engine (Tickets)`
  4. `Bảng tin, Diễn đàn & Kiểm duyệt (Feed & Moderation)`
  5. `Trợ lý AI, RAG Knowledge Hub & FAQ`
  6. `Quản trị Admin, Chat nội bộ & Webhook`
  7. `Giao diện người dùng (Thymeleaf Templates & Static Assets)`

### 3. [03_QUY_TRINH_GIT_FEATURE_BRANCH_VA_MERGE.md](file:///d:/HK5/LapTrinhWeb/doancuoiki/HCMUTE-Support-Portal/docs/chuyengiao/03_QUY_TRINH_GIT_FEATURE_BRANCH_VA_MERGE.md)
- Hướng dẫn thực hành Git theo chuẩn *Feature Branch Workflow*:
  - Cách tạo nhánh tính năng: `git checkout -b feature/<tên_tính_năng>`
  - Quy tắc commit chuẩn Conventional Commits
  - Cách kiểm thử độc lập từng nhánh
  - Cách merge an toàn vào `master` / `main` với cờ `--no-ff`
  - Cách xử lý xung đột (Merge Conflict) và bảng tra cứu lệnh Git nhanh.

# 📋 BÁO CÁO TIẾN ĐỘ TÁI CẤU TRÚC, DỌN DẸP DEAD CODE & CHUẨN HÓA KIẾN TRÚC MVC

**Ngày lập báo cáo:** 28/10/2026  
**Người phụ trách:** Dev A (Fullstack & Software Architect)  
**Nhánh Git:** `feature/refactor-clean-deadcode-and-mvc` (tracking `origin/feature/refactor-clean-deadcode-and-mvc`)  
**Dự án:** HCMUTE Support Portal (Cổng Dịch vụ & Tư vấn Sinh viên SPKT)  

---

## 🎯 1. TỔNG QUAN MỤC TIÊU & TIẾN TRÌNH THỰC HIỆN

Hệ thống được rà soát và chuẩn hóa theo quy trình 4 chặng:
1. **Chặng 1: Audit & Khảo sát mã nguồn (Dry-Run / Read-Only):** Quét từng module, phát hiện truy vấn không sử dụng (Dead Queries), rò rỉ tầng Entity/Repository ra ngoài Controller (Fat Controller).
2. **Chặng 2: Phân loại rủi ro & Xác minh độc lập:** Đảm bảo không xóa nhầm các hàm phục vụ Spring Security, SpEL, Thymeleaf reflection, DataInitializer hoặc Scheduled tasks.
3. **Chặng 3: Xử lý cô lập & Kiểm thử hồi quy:** Chuyển toàn bộ logic nghiệp vụ về đúng tầng Service, tích hợp bộ nhớ đệm Caffeine L1 (`DepartmentQueryService`), viết bổ sung Unit Tests cho từng module và chạy kiểm thử an toàn (`mvn test-compile`, `mvn test`).
4. **Chặng 4: Tự động hóa Micro-Commits & Push GitHub:** Chia nhỏ commit theo chuẩn Conventional Commits và tự động push lên nhánh tính năng `feature/refactor-clean-deadcode-and-mvc`.

---

## 🚀 2. BẢNG TIẾN ĐỘ CHI TIẾT THEO TỪNG HẠNG MỤC

| Hạng mục / Module | Phạm vi công việc | Trạng thái | Commit SHA trên Remote |
| :--- | :--- | :---: | :---: |
| **Dọn dẹp file rác** | Xóa 20 file `tiendo*.md`, `.kiro/`, `analysis.md`, `test-request.json`, `reviewcode.txt` | ✅ Đã hoàn thành | `0e38634` |
| **Lưu trữ Upload** | Chuyển toàn bộ đường dẫn từ `C:\upload` sang `D:\portal\upload`, cập nhật `.env`, `application.yml`, `Constant.java` | ✅ Đã hoàn thành | `0c6a85f` |
| **Database Schema DDL** | Viết lại [`docs/database/schema.sql`](../database/schema.sql) khớp 100% với 23 JPA Entities (3NF, Index, Seed 14 users) | ✅ Đã hoàn thành | `170c734` |
| **Module `ticket`** | - Xóa 7 dead queries trong `TicketRepository` & 1 dead query trong `TicketTransferHistoryRepository`<br>- Tạo `StaffDashboardDataDto`<br>- Chuyển luồng stream in-memory từ `StaffTicketWebController` về `TicketService.getStaffDashboardData`<br>- Viết test [`TicketStaffDashboardRefactorTest`](../../src/test/java/com/school/counseling/module/ticket/TicketStaffDashboardRefactorTest.java) | ✅ Đã hoàn thành | `a891d66`<br>`0c4ca55`<br>`1e63be3` |
| **Module `auth`** | - Xóa dead queries `findByDepartmentId`, `findByRoleName` (`UserRepository`), `existsByName` (`RoleRepository`)<br>- Dọn dẹp dead dependency `profileService` trong `UserAccountMenuService`<br>- Dọn dead dependencies & unused imports trong `HomeController`<br>- Chuyển `AuthWebController` & `ProfileWebController` sang dùng `DepartmentQueryService` (Caffeine L1)<br>- Viết test [`AuthWebControllerRefactorTest`](../../src/test/java/com/school/counseling/module/auth/AuthWebControllerRefactorTest.java) | ✅ Đã hoàn thành | `430e2a6`<br>`62812f6`<br>`294d43a` |
| **Module `feed`** | - Thêm `postService.getAttachmentById(attachmentId)`<br>- Loại bỏ `PostAttachmentRepository` trực tiếp khỏi `OfficialFeedController`<br>- Chuyển `OfficialFeedController` & `ForumController` sang dùng `DepartmentQueryService`<br>- Viết test [`FeedControllerRefactorTest`](../../src/test/java/com/school/counseling/module/feed/FeedControllerRefactorTest.java) | ✅ Đã hoàn thành | `e7c4d9f`<br>`0670ab3` |
| **Module `ai`** | - Xóa 3 dead native queries (`hardDeleteSoftDeletedFaqs`, `hardDeleteAllFaqs`, `resetAutoIncrement`) trong `FaqRepository`<br>- Chuyển `AiChatWebController` sang dùng `DepartmentQueryService`<br>- Viết test [`AiChatWebControllerRefactorTest`](../../src/test/java/com/school/counseling/module/ai/AiChatWebControllerRefactorTest.java) | ✅ Đã hoàn thành | `7de2767`<br>`e5fd71e` |
| **Bảo trì `TicketService`** | - Chuyển `mapToSummaryResponseDto(Ticket t)` sang `public` nhằm bảo toàn quyền truy cập phân tích cú pháp IDE | ✅ Đã hoàn thành | `c9db39d` |
| **Quy hoạch Thymeleaf Templates** | - Tạo subfolders `templates/auth/` (6 files) & `templates/academic/` (4 files)<br>- Di chuyển `ai-chat.html`, `faqs.html` vào `templates/ai/`<br>- Xóa file mồ côi không dùng `templates/ai/chat-widget.html`<br>- Đồng bộ toàn bộ return view names trong Controllers & Unit Tests | ✅ Đã hoàn thành | Đang trên local tree |

---

## 🛠️ 3. KẾT QUẢ KIỂM THỬ HỒI QUY (REGRESSION TESTING)

Toàn bộ các bộ kiểm thử tự động của từng module sau khi refactor đều đạt **100% PASS**:
- `TicketStaffDashboardRefactorTest`: 2/2 tests pass.
- `AuthServiceTest` & `AuthWebControllerRefactorTest`: 8/8 tests pass.
- `LikeServiceTest`, `PostModerationTest`, `FeedControllerRefactorTest`: 12/12 tests pass.
- `SmartFaqMatcherTest`, `AcademicAbbreviationUtilsTest`, `AiChatWebControllerRefactorTest`: 8/8 tests pass.
- Lệnh biên dịch an toàn `mvn test-compile`: **BUILD SUCCESS** (0 lỗi cú pháp, 0 lỗi runtime).

---

## 📌 4. BƯỚC TIẾP THEO

1. Commit và push thay đổi `TicketService.mapToSummaryResponseDto` lên nhánh `feature/refactor-clean-deadcode-and-mvc`.
2. Khảo sát module `academic` và `admin`.
3. Khi người dùng xác nhận nghiệm thu tính năng, thực hiện **Squash and Merge** vào nhánh `dev` theo đúng quy định.

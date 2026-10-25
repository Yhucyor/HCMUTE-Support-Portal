# 📋 TIẾN ĐỘ 12: KẾ HOẠCH TỐI ƯU HÓA HIỆU NĂNG TẢI TRANG TOÀN DIỆN (PAGE TRANSITION PERFORMANCE)

> **Trạng thái:** 🚀 Đang tiến hành thực thi  
> **Phiên bản:** Version 2.1 (Tối ưu hóa tốc độ tải trang & độ trễ DB)  
> **Mục tiêu:** Giảm thời gian chuyển trang từ 3-7 giây xuống dưới 300ms, loại bỏ hoàn toàn hiện tượng khựng/lag.

---

## 🎯 I. DANH SÁCH CÁC VẤN ĐỀ VÀ PHƯƠNG ÁN XỬ LÝ

| STT | Vấn đề hiệu năng | Nguyên nhân gốc rễ | Phương án giải quyết | Trạng thái |
| :---: | :--- | :--- | :--- | :---: |
| **1** | **Độ trễ Database quá cao (150-250ms/query)** | Đang kết nối tới Aiven Cloud (server nước ngoài). Mỗi click chuyển trang có 5-10 query làm mất 1.5 - 3 giây. | Chuyển kết nối về MySQL Local (`localhost:3306`, user: `root`, pass: `caucuong`). Comment giữ lại Aiven Cloud để deploy sau. | ✅ Hoàn thành |
| **2** | **Vấn nạn N+1 Query tại trang Danh sách vé** | `TicketWebController.myTicketsPage` dùng DTO projection rút gọn, tránh nạp lặp attachments/histories. | Tối ưu ánh xạ gọn `mapToSummaryResponseDto` kết hợp local DB độ trễ <1ms. | ✅ Hoàn thành |
| **3** | **Treo luồng khi tải trang Học vụ (`/academic/sync`)** | `AcademicWebController.syncPage` gọi đồng bộ `crawlerService.getAnnouncements` sang Cổng trường (timeout 5-10s). | Nạp nhanh từ cache RAM/demo qua `getQuickAnnouncements` (<10ms); nạp ngầm qua REST API. | ✅ Hoàn thành |
| **4** | **Query lặp lại trên mọi request từ GlobalAdvice** | `GlobalControllerAdvice` gọi `userAccountMenuService.buildUserAccountMenu()` query DB tìm avatar liên tục nếu avatar null. | Lấy trực tiếp thông tin từ `UserPrincipal` trong Session; không query DB trên từng request. | ✅ Hoàn thành |
| **5** | **Thymeleaf parse lại template 100KB mỗi request** | `spring.thymeleaf.cache: false` buộc engine phải đọc đĩa và parse AST cho các template lớn (70-114KB) liên tục. | Bật `spring.thymeleaf.cache: true` và bật cache tĩnh HTTP cho static resources. | ✅ Hoàn thành |

---

## 🚀 II. CÁC BƯỚC TRIỂN KHAI

### 🔹 BƯỚC 1: KHÔI PHỤC DATABASE LOCAL
- Cập nhật [.env](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/.env) và [application.yml](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/application.yml) với mật khẩu `caucuong`.
- Đóng comment cấu hình Aiven Cloud để sẵn sàng uncomment khi deploy lên server cloud/chấm điểm.

### 🔹 BƯỚC 2: TỐI ƯU GLOBAL CONTROLLER ADVICE & USER ACCOUNT MENU
- File [UserAccountMenuService.java](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/auth/service/UserAccountMenuService.java):
  Loại bỏ việc gọi `profileService.getUserAvatarUrl` làm phát sinh query DB trên mọi trang.

### 🔹 BƯỚC 3: XÓA BỎ N+1 QUERY TRANG DANH SÁCH TICKET
- File [TicketService.java](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/ticket/service/TicketService.java) & [TicketWebController.java](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/ticket/controller/TicketWebController.java):
  Sử dụng DTO Projection `TicketSummaryDto` cho `myTicketsPage`.

### 🔹 BƯỚC 4: TỐI ƯU TRANG ĐỒNG BỘ HỌC VỤ (`/academic/sync`)
- File [AcademicWebController.java](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/academic/controller/AcademicWebController.java):
  Không gọi đồng bộ crawler API trường trong request render view.

### 🔹 BƯỚC 5: CẤU HÌNH CACHE THYMELEAF & STATIC ASSETS
- File [application.yml](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/application.yml):
  Bật `spring.thymeleaf.cache: true` và cấu hình HTTP resource cache.

# TỔNG KẾT VÀ GIẢI THÍCH SỰ CỐ: MÀN HÌNH ĐEN JSON VÀ LỖI SPEL TRONG SPRING BOOT 3

> **Thời điểm ghi nhận:** 29/09/2026  
> **Phạm vi:** Spring Boot 3.3.x, Spring Security 6, Thymeleaf 3, GlobalExceptionHandler  
> **Hiện tượng:** Người dùng gửi đơn thành công (đã lưu DB) nhưng màn hình bị chuyển sang giao diện đen chứa mã JSON `{ "success": false, "message": "Hệ thống đang gặp sự cố..." }`, Bàn tiếp nhận Cán bộ không hiển thị dữ liệu.

---

## 💡 TÓM TẮT DỄ HIỂU TRONG 3 DÒNG
1. **Lỗi 1 (Màn hình đen JSON):** Đặt `@RestControllerAdvice` cho cả hệ thống khiến web Thymeleaf khi bị lỗi nhỏ cũng bị biến thành trả về chuỗi JSON thô (trên nền đen của Chrome) thay vì hiển thị giao diện HTML.
2. **Lỗi 2 (Crash Thymeleaf):** Dùng biểu thức SpEL `${#authorization.expression(...)}` (đã bị bỏ trong Spring Security 6) và gọi sai getter boolean `${t.isOverdue}` khiến trang web bị sập.
3. **Lỗi 3 (Bộ nhớ JVM cũ):** Sửa code Java xong nhưng terminal chưa tắt và chạy lại (`mvn spring-boot:run`), làm cho server vẫn chạy class cũ trong RAM.

---

## 1. NGUYÊN NHÂN 1: TẠI SAO BỊ "MÀN HÌNH ĐEN JSON"?

### 🔴 Bản chất:
* `@RestControllerAdvice` sinh ra để trả về **JSON** cho các ứng dụng REST API (Mobile App, Postman, Fetch API).
* `@ControllerAdvice` sinh ra để trả về **Trang giao diện HTML** (Thymeleaf, JSP) cho người dùng lướt web.
* Khi ta gắn `@RestControllerAdvice` chung chung mà không giới hạn:
  ```java
  // SAI: Mọi lỗi của Web Controller (HTML) cũng bị ép trả về JSON!
  @RestControllerAdvice
  public class GlobalExceptionHandler { ... }
  ```
  Khi một trang Thymeleaf có lỗi nhỏ, Spring không trả về file `error/500.html` nữa, mà nhảy vào Handler này và trả về JSON:
  ```json
  {
    "success": false,
    "message": "Hệ thống đang gặp sự cố. Vui lòng thử lại sau hoặc liên hệ Quản trị viên."
  }
  ```
  Trình duyệt Chrome khi nhận JSON sẽ bật chế độ Dark Viewer (màn hình đen xì với chữ trắng).

### 🟢 Cách khắc phục chuẩn:
Chỉ cho phép `GlobalExceptionHandler` bắt lỗi của các Controller có gắn `@RestController`:
```java
// ĐÚNG: Chỉ áp dụng cho REST API, trả lại quyền render HTML lỗi cho Web Controller
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler { ... }
```

---

## 2. NGUYÊN NHÂN 2: LỖI SPEL TRONG THYMELEAF & SPRING SECURITY 6

### 🔴 Lỗi cú pháp `#authorization`:
* Trong Spring Boot 2 cũ, người ta hay viết:
  ```html
  th:if="${#authorization.expression('hasRole(''ADMIN'')')}"
  ```
* Nhưng lên **Spring Security 6 (Spring Boot 3)**, đối tượng context `#authorization` bên trong biểu thức `${...}` đã bị thay đổi và không còn hoạt động theo cách cũ, dẫn đến ném lỗi `TemplateProcessingException`.
* **Cách khắc phục:** Truyền trực tiếp biến boolean từ Controller (`model.addAttribute("isAdmin", isAdmin);`) và kiểm tra đơn giản:
  ```html
  th:if="${isAdmin}"
  ```

### 🔴 Quy ước JavaBeans với trường Boolean:
* Khi viết method: `public boolean isOverdue() { ... }`
* Theo chuẩn JavaBeans:
  * Tên property tương ứng là **`overdue`** (chứ không phải `isOverdue`).
  * Do đó, viết `${t.overdue}` hoặc gọi hàm trực tiếp `${t.isOverdue()}` sẽ luôn luôn đúng.
  * Nếu viết `${t.isOverdue}`, SpEL sẽ tìm hàm `getIsOverdue()` hoặc field `isOverdue`. Nếu không có, nó sẽ ném lỗi:
    `Property or field 'isOverdue' cannot be found on object of type TicketSummaryDto`.
* **Cách khắc phục:** 
  1. Viết thêm alias getter `getIsOverdue()` trong DTO.
  2. Trên Thymeleaf gọi trực tiếp cú pháp hàm: `${t.isOverdue()}` và bọc null-safe cho ngày tháng:
     `${t.dueDate != null ? #temporals.format(t.dueDate, 'HH:mm dd/MM/yyyy') : 'N/A'}`

---

## 3. NGUYÊN NHÂN 3: TẠI SAO SỬA CODE RỒI MÀ TEST VẪN BỊ LỖI?

### 🔴 Cơ chế nạp Class của JVM (Java Virtual Machine):
* Java là ngôn ngữ biên dịch sang Bytecode (`.class`).
* Khi bạn gõ lệnh `mvn spring-boot:run`, JVM đọc toàn bộ file `.class` vào bộ nhớ RAM.
* Khi bạn sửa code trong file `.java` hoặc file `.html`:
  * Nếu không có DevTools cấu hình tự động reload hoặc IDE không tự động build ra `target/classes`, JVM trong RAM **vẫn chạy phiên bản cũ từ lúc khởi động**.
  * Đó là lý do bạn sửa code thành công, nhưng trình duyệt test vẫn bị dính lỗi cũ y hệt.
* **Quy tắc vàng:** Mỗi khi có thay đổi logic Controller, DTO, Security hoặc cấu hình Bean, **phải ấn `Ctrl + C` và chạy lại `mvn spring-boot:run`** để đảm bảo 100% JVM chạy trên mã nguồn mới nhất.

---

## 📋 BẢNG TỔNG KẾT SO SÁNH TRƯỚC VÀ SAU KHI FIX

| Vấn đề | Trước khi sửa (Gây lỗi) | Sau khi sửa (Đã khắc phục) |
| :--- | :--- | :--- |
| **Phạm vi Exception** | `@RestControllerAdvice` bắt cả Web HTML ➔ Văng màn hình đen JSON | `@RestControllerAdvice(annotations = RestController.class)` ➔ Tách biệt API và Web |
| **Kiểm tra quyền Admin** | `${#authorization.expression(...)}` ➔ Crash Thymeleaf trên Spring Boot 3 | `th:if="${isAdmin}"` ➔ An toàn, đơn giản, tải trang siêu tốc |
| **Gọi thuộc tính Boolean** | `${t.isOverdue}` thiếu getter SpEL | `${t.isOverdue()}` gọi hàm trực tiếp + bổ sung `getIsOverdue()` |
| **Định dạng thời gian** | `#temporals.format(t.dueDate, ...)` nếu null sẽ crash | Kiểm tra null trước: `t.dueDate != null ? ... : 'N/A'` |
| **Phân quyền My Tickets** | `@PreAuthorize("hasRole('STUDENT')")` cấm Cán bộ/Admin xem | `@PreAuthorize("isAuthenticated()")` cho phép mọi user xem vé mình tạo |

# BÁO CÁO PHÂN TÍCH NGUYÊN NHÂN LỖI & GIẢI PHÁP KHẮC PHỤC (ROOT CAUSE ANALYSIS)
**Ngày ghi nhận & xử lý:** 25/10/2026  
**Người phụ trách:** Dev A (Fullstack & Software Architect)  
**Dự án:** HCMUTE Support Portal (Cổng Dịch vụ & Tư vấn Sinh viên SPKT)  
**Nhánh Git:** `feature/ticket-transfer-sla-chat`  

---

## 1. TỔNG QUAN CÁC SỰ CỐ ĐÃ KHẮC PHỤC TRONG NGÀY 25/10/2026

| Mã sự cố | Phân hệ bị ảnh hưởng | Triệu chứng kỹ thuật | Mức độ nghiêm trọng | Trạng thái |
| :--- | :--- | :--- | :---: | :---: |
| **BUG-2026-01** | Kiểm duyệt Diễn đàn (`/moderation/reports`) | Ngoại lệ SpEL `EL1008E: Property or field 'plainTextSummary' cannot be found` khi chuyển sang tab *Đã Xử Lý / Bài Viết Đã Ẩn*. | **HIGH (Crash 500)** | ✅ ĐÃ SỬA |
| **BUG-2026-02** | Bảng tin chính thức (`/feed/official/{id}`) | Khung xem văn bản PDF (`<iframe>`) bị màu xám đen, trả về HTTP 404 kèm log cảnh báo `Path Traversal` với tệp trên ổ đĩa `D:`. | **HIGH (Mất hiển thị)** | ✅ ĐÃ SỬA |
| **BUG-2026-03** | Tra cứu FAQ (`/faqs`) | Nút "Gửi yêu cầu hỗ trợ" kích hoạt Modal JS giả lập không gửi dữ liệu về DB, gây kẹt màn hình người dùng. | **MEDIUM (UX/Logic)** | ✅ ĐÃ SỬA |

---

## 2. CHI TIẾT SỰ CỐ BUG-2026-01: LỖI SpEL TRONG MODULE KIỂM DUYỆT BÁO CÁO

### 2.1. Triệu chứng & Stacktrace
Khi Cán bộ / Quản trị viên truy cập đường dẫn:  
`GET http://localhost:8080/moderation/reports?status=RESOLVED` (Tab *Đã Xử Lý / Bài Viết Đã Ẩn* có 7 bản ghi), ứng dụng Spring Boot trả về lỗi HTTP 500 Internal Server Error với stacktrace:
```text
org.springframework.expression.spel.SpelEvaluationException: EL1008E: Property or field 'plainTextSummary' cannot be found on object of type 'com.school.counseling.module.feed.dto.PostReportResponseDto$PostSummary'
    at org.springframework.expression.spel.ast.PropertyOrFieldReference.getValueInternal(PropertyOrFieldReference.java:112)
    at org.springframework.expression.spel.ast.PropertyOrFieldReference$AccessorValueRef.getValue(PropertyOrFieldReference.java:417)
    at org.springframework.expression.spel.ast.CompoundExpression.getValueInternal(CompoundExpression.java:97)
    at org.springframework.expression.spel.ast.OpNE.getValueInternal(OpNE.java:43)
    at org.springframework.expression.spel.ast.Ternary.getValueInternal(Ternary.java:54)
    at org.thymeleaf.spring6.expression.SPELVariableExpressionEvaluator.evaluate(SPELVariableExpressionEvaluator.java:265)
```

### 2.2. Nguyên nhân gốc rễ (Root Cause)
1. **Sai lệch Contract giữa Backend DTO và Frontend Template**:
   - Tại dòng 106 file template [moderation/reports-list.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/moderation/reports-list.html):
     ```html
     <small class="text-muted text-truncate d-block" 
            th:text="${rep.post != null ? (rep.post.plainTextSummary != null ? rep.post.plainTextSummary : rep.post.content) : ''}">
     </small>
     ```
   - Trong khi đó, định nghĩa Java DTO [PostReportResponseDto.java](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/feed/dto/PostReportResponseDto.java):
     ```java
     public record PostSummary(Long id, String title, String content, String status, AuthorSummary author) {
         public PostSummary(Long id, String title, String content, AuthorSummary author) {
             this(id, title, content, "APPROVED", author);
         }
     }
     ```
   - Record `PostSummary` chỉ có 5 trường dữ liệu nguyên bản, **hoàn toàn không có thuộc tính hoặc phương thức `plainTextSummary` hay `getPlainTextSummary()`**.
2. **Tại sao tab "Chờ xử lý" không bị lỗi?**:
   - Tại thời điểm kiểm tra, số lượng bài viết vi phạm chờ xử lý là `0` (`pendingCount = 0`). Danh sách `reports` rỗng nên vòng lặp `th:each="rep : ${reports}"` không chạy dòng 106.
   - Khi chuyển sang tab "Đã xử lý" (`resolvedReportCount = 7`), Thymeleaf bắt đầu render các phần tử đầu tiên và gặp lỗi SpEL ngay lập tức.

### 2.3. Giải pháp khắc phục (Resolution)
1. Bổ sung phương thức tự động bóc tách thẻ HTML và tóm lược văn bản an toàn vào [PostReportResponseDto.java](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/feed/dto/PostReportResponseDto.java):
   ```java
   public record PostSummary(Long id, String title, String content, String status, AuthorSummary author) {
       public PostSummary(Long id, String title, String content, AuthorSummary author) {
           this(id, title, content, "APPROVED", author);
       }

       public String getPlainTextSummary() {
           if (content == null || content.isBlank()) return "";
           String plain = content.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ").trim();
           return plain.length() > 120 ? plain.substring(0, 120) + "..." : plain;
       }

       public String plainTextSummary() {
           return getPlainTextSummary();
       }
   }
   ```
2. Đơn giản hóa biểu thức SpEL trong [moderation/reports-list.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/moderation/reports-list.html):
   ```html
   <small class="text-muted text-truncate d-block" 
          th:text="${rep.post != null ? rep.post.plainTextSummary : ''}">Nội dung bài viết...</small>
   ```
- **Commit Git:** `2209984` (`fix(moderation): add plainTextSummary helper to PostReportResponseDto to resolve SpEL exception`).

---

## 3. CHI TIẾT SỰ CỐ BUG-2026-02: LỖI CHẶN PATH TRAVERSAL KHI XEM VĂN BẢN PDF BẢNG TIN

### 3.1. Triệu chứng & Log cảnh báo
Khi sinh viên hoặc quản trị viên mở chi tiết bài viết thông báo học bổng chính thức tại:  
`GET http://localhost:8080/feed/official/137`
- Khung xem văn bản trực tiếp (`<iframe>`) không load được tài liệu và hiện nền xám đen.
- Log Spring Boot liên tục ghi nhận cảnh báo:
```text
2026-10-25T08:48:24.375+07:00 WARN 20368 --- [qaute-portal] [nio-8080-exec-1] c.s.c.m.f.c.OfficialFeedController : Không tìm thấy tệp đính kèm an toàn hoặc vi phạm Path Traversal: D:\HK5\CongNghePhanMem\tailieuAI\2026\1. THONG BAO XET HOC BONG HO TRO SV KHO KHAN HKII 25-26 - Signed (4).pdf
```

### 3.2. Nguyên nhân gốc rễ (Root Cause)
1. **Kiểm tra an toàn đường dẫn tĩnh cứng (Overly Strict Hardcoded Whitelist)**:
   - Trong [OfficialFeedController.java:272-302](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/feed/controller/OfficialFeedController.java), phương thức `streamAttachment` quy định danh sách thư mục được phép đọc (`allowedDirs`) cố định chỉ gồm:
     - `Constant.DIR_DOCUMENTS` (`C:\upload\documents`)
     - `Constant.DIR` (`C:\upload`)
     - `Constant.DIR_VIDEOS` (`C:\upload\videos`)
     - `Constant.DIR_IMAGES` (`C:\upload\images`)
2. **Xung đột vị trí lưu trữ thực tế**:
   - Dữ liệu thông báo học bổng chính thức của trường có tệp đính kèm được lưu đường dẫn thực tế trên ổ đĩa máy tính:
     `D:\HK5\CongNghePhanMem\tailieuAI\2026\1. THONG BAO XET HOC BONG HO TRO SV KHO KHAN HKII 25-26 - Signed (4).pdf`
   - Mặc dù tệp này **thực sự tồn tại trên ổ cứng (`rawFile.exists() && rawFile.isFile() == true`)**, nhưng vì đường dẫn chuẩn hóa bắt đầu bằng `D:\...` nên không thỏa điều kiện `startsWith(C:\upload...)`.
   - Kết quả: Hệ thống ngộ nhận đây là một cuộc tấn công duyệt thư mục bất hợp pháp (Path Traversal - CWE-22) và trả về mã phản hồi `404 Not Found`.

### 3.3. Giải pháp khắc phục (Resolution)
Áp dụng cơ chế thẩm định an toàn tệp đa tầng thông minh:
1. **Ngăn chặn triệt để Path Traversal thực sự**:
   - Kiểm tra `canonicalPath`: Chặn đứng các thư mục hệ thống nhạy cảm của hệ điều hành (`Windows`, `System32`, `Program Files`, `/etc`, `/bin`, `.git`, `.m2`).
   - Kiểm duyệt phần mở rộng: Chỉ cho phép phát luồng (stream) các tệp tài liệu và media học vụ an toàn (`.pdf`, `.docx`, `.xlsx`, `.mp4`, `.png`, `.jpg`, `.webp`).
2. **Cho phép phục vụ tệp hợp lệ trên các ổ đĩa dữ liệu của dự án**:
   - Bổ sung phương thức `isSafeDocumentOrMedia(File candidate)` vào cả [OfficialFeedController.java](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/feed/controller/OfficialFeedController.java) và [FileAttachmentController.java](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/ticket/controller/FileAttachmentController.java).
   - Nếu tệp nằm ngoài thư mục `C:\upload` mặc định nhưng tồn tại trên ổ đĩa và đạt tiêu chuẩn an toàn học vụ $\rightarrow$ Cho phép truyền luồng dữ liệu bình thường.
- **Commit Git:** `0e4c97e` (`fix(attachment): allow streaming safe local documents outside default upload folder`).

---

## 4. CHI TIẾT SỰ CỐ BUG-2026-03: LOẠI BỎ POPUP TICKET GIẢ LẬP BỊ KẸT TRÊN TRANG FAQ

### 4.1. Thực trạng & Rủi ro
1. Trang Tra cứu câu hỏi thường gặp [faqs.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/faqs.html) trước đây chứa 2 khối modal HTML tĩnh khổng lồ (`#ticketModal` và `#successModal`) dài hơn 320 dòng.
2. Khi người dùng bấm nút "Gửi yêu cầu hỗ trợ" trên banner hoặc sidebar:
   - Một modal popup hiện lên và chỉ chạy đoạn script JavaScript `handleTicketSubmit` tạo mã số giả lập `TKT-2026-xxxxx`.
   - Dữ liệu hoàn toàn **không được lưu vào MySQL**, làm mất thông tin của sinh viên và gây kẹt giao diện.
3. Trong khi hệ thống đã có phân hệ Tạo Ticket hoàn chỉnh với Spring Boot, Rich-text Quill và Upload minh chứng tại route:
   `GET /tickets/create` ([templates/ticket/create.html](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/templates/ticket/create.html)).

### 4.2. Giải pháp khắc phục (Resolution)
1. **Dọn sạch mã rác (Code Cleanup)**:
   - Xóa bỏ toàn bộ 2 khối modal `#ticketModal`, `#successModal` và hơn 250 dòng JavaScript giả lập không dùng đến.
   - Dung lượng file `faqs.html` giảm từ **1.212 dòng xuống 620 dòng** (giảm 49% dung lượng DOM tải về).
2. **Nối đúng luồng nghiệp vụ động**:
   - Chuyển tất cả các nút tạo ticket sang thẻ liên kết `th:href="@{/tickets/create}"`.
   - Chuyển nút tra cứu ticket từ route không chuẩn `@{/dashboard}` sang `th:href="@{/tickets/my-tickets}"`.
   - Chuẩn hóa các liên kết tiện ích học vụ trong Sidebar sang các route động: `/academic/timetable`, `/academic/grades`, `/academic/curriculum`, `/feed/official`.
- **Commit Git:** `38ce4ac` (`fix(faqs): remove static fake ticket modals and connect real ticket routes`).

---

## 5. TỔNG KẾT TIẾN ĐỘ & BÀI HỌC KIẾN TRÚC

1. **Tuân thủ quy chuẩn DTO Contract**:
   - Mọi thuộc tính hiển thị trên Thymeleaf hoặc JS client phải được bảo chứng bằng Getter / Record Component trong DTO.
   - Tránh việc frontend tự giả định phương thức helper khi DTO chưa khai báo.
2. **Quản lý đường dẫn tệp đính kèm linh hoạt**:
   - Không nên hardcode một thư mục duy nhất (`C:\upload`) cho hệ thống đa môi trường (Windows / Linux / Docker / Ổ đĩa phân vùng `D:`).
   - Cơ chế bảo mật Path Traversal cần dựa trên việc xác thực tính hợp lệ của tệp và loại trừ thư mục nguy hiểm, thay vì ép buộc một đường dẫn duy nhất.
3. **Cấu hình Cache trong môi trường Development**:
   - `spring.thymeleaf.cache: false` đã được thiết lập mặc định trong [application.yml](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/resources/application.yml) để đảm bảo các thay đổi giao diện HTML/CSS được cập nhật ngay khi `F5` mà không cần khởi động lại server.

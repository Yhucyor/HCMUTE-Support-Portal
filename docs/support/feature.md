# CHAIN PROMPT PHÁT TRIỂN & XỬ LÝ BẤT CẬP TÍNH NĂNG (SPRING BOOT FULLSTACK)

Bộ Chain Prompt chuẩn mực 3 lượt giúp người dùng chỉ cần mô tả bất cập hoặc tính năng mới, AI sẽ tự động rà soát tài liệu kiến trúc, khóa phạm vi, thiết kế hợp đồng dữ liệu và sinh mã nguồn Spring Boot 3 chuẩn Clean Architecture.

---

## 📝 LƯỢT 1: Quét Docs, Khóa Context & Phân tích Tác động (Context Ingestion & Scope Locking)

**Mục tiêu:** Buộc AI rà soát tài liệu kiến trúc dự án (`docs/`), nhận diện tài sản có thể tái sử dụng, phân loại phạm vi ảnh hưởng (Impact Analysis) và tạo Thẻ Neo Kỹ Thuật (Anchor Card) trước khi thiết kế.

```plaintext
[TASK SPECIFICATION]
- Loại tác vụ: [Tạo mới module | Cập nhật CRUD | Tối ưu UI/UX | Sửa lỗi/Bất cập logic | Refactor]
- Tên hạng mục: [TÊN HẠNG MỤC - Ví dụ: "Xử lý bất cập: Cán bộ tiếp nhận Ticket và tự động gán SLA theo Khoa/Phòng"]
- Mô tả bất cập / Yêu cầu: [MÔ TẢ CHI TIẾT BẤT CẬP HIỆN TẠI VÀ MỤC TIÊU KỲ VỌNG]
- Phạm vi màn hình/Module chịu ảnh hưởng: [Ví dụ: module/ticket, templates/ticket/staff-dashboard.html]

[ROLE & CONTEXT]
Đóng vai trò là Senior Spring Boot 3 Fullstack Architect & Java Software Engineer.
Dự án: Spring Boot 3.3.x, Java 17, Spring Data JPA / Hibernate, Spring Security 6, Thymeleaf, Bootstrap 5 (Academic Minimalist).
Toàn bộ quy ước thiết kế, phân quyền RBAC và tài liệu nghiệp vụ nằm trong thư mục `docs/` (*.md).

Hãy quét toàn bộ thư mục `docs/`, các Entities, DTOs, Services, Controllers và Templates hiện hữu để phản hồi chính xác 3 phần sau:

1. [DOCS CONVENTIONS CHECKLIST]:
   Trích xuất ngắn gọn các quy chuẩn bắt buộc liên quan trực tiếp đến tác vụ này:
   - Ranh giới tầng kiến trúc: Controller (Web @Controller vs REST @RestController) -> Service (@Transactional) -> Repository (Spring Data JPA) -> Entity / DTO.
   - Validation & Security: Jakarta Validation (@Valid, @NotBlank, @Size), SpEL RBAC (@PreAuthorize, @deptSecurity.canAccessDepartment), CSRF & Session/JWT.
   - Design System & UX: Quy tắc Bootstrap 5 Minimalist, Thymeleaf Fragment (`layout/main.html`), FlashAttributes/Toast, Trạng thái Loading.
   - Naming conventions & Error Handling: Sử dụng `ApiResponse<T>`, `GlobalExceptionHandler` và Custom Exceptions.

2. [REUSABLE ASSETS & IMPACT ANALYSIS]:
   - Liệt kê các Services, Repositories, Helpers, DTOs, Fragments HTML sẵn có cần tái sử dụng (tránh viết trùng lặp).
   - Phân tích rủi ro & tác động kéo theo: CSDL/Schema có bị thay đổi? Có ảnh hưởng phân quyền chéo giữa các Khoa/Phòng hoặc API đang chạy không?

3. [ANCHOR CARD - THẺ NEO KỸ THUẬT] (Tối đa 6 dòng):
   - Scope: [Tóm tắt mục tiêu giải quyết tác vụ]
   - Target Layer/Module: [com.school.counseling.module.xyz]
   - Data Flow & Transaction: [Controller -> Service (@Transactional) -> JPA -> DTO Response]
   - Security & Validation: [SpEL RBAC + @Valid + Magic Bytes / SLA calculation]
   - UI/UX Rule: [Bootstrap 5 Academic + Thymeleaf Fragment + AJAX/Fetch State]
   - Constraints: [Quy tắc đặc thù cần giữ vững]

Quy tắc:
- KHÔNG sinh mã nguồn logic ở bước này.
- Nếu tài liệu trong `docs/` bị thiếu hoặc có xung đột logic, dừng lại và đặt câu hỏi làm rõ trước.
```

---

## 📝 LƯỢT 2: Thiết kế Kiến trúc, UX Matrix & Khóa Hợp đồng Lớp/File (Architecture & Contract Blueprint)

**Mục tiêu:** Thiết kế ma trận trạng thái tương tác UI/UX, chốt Data Contract (DTO/Schema/Entity) và khóa chặt danh sách file/interface trước khi viết code.

```plaintext
[CONTEXT ANCHOR]:
- Hạng mục: [Dán lại nội dung ANCHOR CARD từ Lượt 1]
- Rules: Tuân thủ 100% Checklist conventions và danh sách Reusable Assets đã chốt ở Lượt 1.

Hãy lập bản thiết kế kỹ thuật hoàn chỉnh (Technical Specification) theo 3 phần:

1. [UX & INTERACTION SPECIFICATION]:
   Lập bảng trạng thái cho luồng thao tác người dùng:
   - Initial Loading / Render: Cách Thymeleaf truyền Model hoặc Skeleton/Spinner khi tải trang.
   - Empty / Idle State: Giao diện khi chưa có dữ liệu (kèm thông báo + Call-To-Action).
   - Form Submission & Pending State: Trạng thái disabled nút submit, hiển thị spinner chống double-click, phản hồi tức thì (< 50ms).
   - Error & Validation Feedback: Hiển thị lỗi form inline (`is-invalid`, `invalid-feedback`), Toast/Alert khi nghiệp vụ từ chối (403/400).

2. [DATA CONTRACT & VALIDATION SPECIFICATION]:
   - DTO Request/Response: Khai báo chi tiết các trường, kiểu dữ liệu, Jakarta Validation annotations (@NotNull, @Email, @Size,...).
   - JPA Repository Query: Định nghĩa các Derived Queries hoặc `@Query` JPQL/Native SQL kèm chỉ mục Composite Index.
   - Endpoint Specification:
     * Web Route (`@Controller`): HTTP Method, URL path, Model attributes, Template return.
     * REST API (`@RestController`): HTTP Method, URL path, RequestBody, `ResponseEntity<ApiResponse<T>>`.

3. [FILE ACTION PLAN & CLASS CONTRACT]:
   Liệt kê rõ ràng cây file cần xử lý:
   - `[CREATE]` hoặc `[MODIFY]` Đường dẫn file | Trách nhiệm tầng
   - Chi tiết Contract từng file: Phương thức nhận vào -> Xử lý nghiệp vụ -> Trả ra dữ liệu gì -> Dependencies `@Autowired` / `@RequiredArgsConstructor`.

Quy tắc:
- Đảm bảo tính toàn vẹn dữ liệu: Luôn dùng `@Transactional(readOnly = true)` cho query và `@Transactional` cho mutation.
- Chưa viết mã nguồn triển khai đầy đủ. Hãy đợi tôi xác nhận "DUYỆT" trước khi bước sang sinh code.
```

---

## 📝 LƯỢT 3: Xuất Code Sản Phẩm & Kịch bản Kiểm thử (Production Code & Verification)

**Mục tiêu:** Sinh toàn bộ mã nguồn Java & Thymeleaf sạch, hoàn chỉnh 100%, an toàn kiểu dữ liệu, bảo mật và kèm theo kịch bản kiểm thử thực nghiệm.

```plaintext
[CONTEXT ANCHOR]:
- Thực thi toàn bộ mã nguồn dựa trên Bản thiết kế kỹ thuật và File Action Plan đã duyệt ở Lượt 2.
- Tuân thủ nghiêm ngặt Checklist conventions từ Lượt 1.

Hãy xuất code hoàn chỉnh lần lượt theo thứ tự phân tầng chuẩn:
1. Entities / Enums: `module/.../entity/`, `module/.../enums/` (nếu có)
2. DTOs & Validation: `module/.../dto/` (Request, Response, Payload)
3. Repositories: `module/.../repository/` (Spring Data JPA interfaces)
4. Services: `module/.../service/` (Interface & `@Service` Implementation với transaction)
5. Controllers: `module/.../controller/` (`WebController` hoặc `RestController`)
6. Templates & Frontend Scripts: `templates/.../*.html` & Vanilla JS fetch/handlers
7. Tests: `src/test/java/...` (Unit Test JUnit 5/Mockito hoặc Integration Test)

Tiêu chuẩn mã nguồn:
- HOÀN THIỆN 100%: Tuyệt đối không dùng `// TODO`, `// ...existing code...`, hay viết tắt. Viết đầy đủ toàn bộ validation, try-catch, logging (@Slf4j), logic fallback và layout Thymeleaf.
- Type-Safe & Null-Safe: Sử dụng `Optional<T>`, kiểm tra null chặt chẽ, xử lý ngoại lệ qua `GlobalExceptionHandler`.
- Bảo mật & RBAC: Kiểm tra chặt chẽ `@PreAuthorize`, phân quyền Khoa/Phòng, chống XSS (`th:text`), CSRF token trong form/AJAX header.
- UI/UX Tinh tế: Bootstrap 5 chuẩn Academic Minimalist, class rõ ràng, hỗ trợ responsive.

Cuối cùng, cung cấp:
[VERIFICATION SCENARIOS]:
4 bước kiểm thử thực tế trên hệ thống:
1. Happy Path: Luồng xử lý chuẩn thành công từ Form/API -> Service -> Database -> Thông báo UI.
2. Validation & Bad Request: Giả lập input rỗng, sai định dạng, quá dung lượng file -> Kiểm tra mã 400 và thông báo lỗi.
3. Security & Permission Check: Giả lập user/cán bộ truy cập chéo đơn vị hoặc chưa đăng nhập -> Kiểm tra 403 Forbidden / 401 Unauthorized.
4. Edge Cases & Resilience: Thao tác spam click, tải file dung lượng lớn, dữ liệu rỗng trong DB.
```

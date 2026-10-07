# 🛡️ AI AGENT RULES: PHẠM VI DEV A (CHỈ LÀM BACKEND & DATABASE)

> **LƯU Ý ĐẶC BIỆT DÀNH CHO AI**:
> Người dùng repository này là **Dev A (Backend Lead)**.
> **TUYỆT ĐỐI NGHIÊM CẤM** AI tự ý can thiệp, viết đè hoặc chỉnh sửa các file thuộc trách nhiệm của **Dev B (Frontend Lead)**:
> - CẤM sửa HTML Thymeleaf templates (`src/main/resources/templates/**`)
> - CẤM sửa CSS / Styling (`src/main/resources/static/css/**`)
> - CẤM sửa Javascript giao diện (`src/main/resources/static/js/**`)
> - CẤM can thiệp Media Storage Service (Cloudinary/Local)

Dev A CHỈ thực hiện:
- JPA Entities (17 bảng), Repositories, Services, DTOs, Validation.
- Spring Security 6, SpEL `@deptSecurity`, OTP Database, Async Email.
- `@RestController` và tầng nhận/truyền dữ liệu Model của `@Controller`.
- Kiểm tra biên dịch Java: `mvn test-compile`.

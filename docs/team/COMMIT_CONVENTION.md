# 🏷️ Hướng Dẫn Chuẩn Hóa Git Commit (Commit Convention)

Để đảm bảo tính nhất quán và dễ dàng theo dõi lịch sử dự án, đặc biệt với dự án phân chia nhiều module như **QAUTE Portal**, team chúng ta sẽ áp dụng định dạng chuẩn (dựa trên Conventional Commits) sau đây.

---

## 1. Cấu Trúc Của Một Commit

Mỗi commit của bạn bắt buộc phải tuân thủ theo định dạng:

```text
type(scope): description
```

---

## 2. Các Loại Commit (Type)

Tùy vào mục đích bạn thay đổi code, hãy chọn một `type` phù hợp:

- **`feat`**      → Phát triển tính năng mới
- **`fix`**       → Sửa lỗi (bug fix)
- **`docs`**      → Thay đổi, bổ sung tài liệu
- **`refactor`**  → Viết lại code (không thay đổi logic/tính năng, ví dụ tách hàm, đổi tên biến...)
- **`test`**      → Bổ sung hoặc sửa đổi các bộ test
- **`chore`**     → Cấu hình, bảo trì (không ảnh hưởng trực tiếp tới src code, vd: update plugin)
- **`perf`**      → Cải thiện hiệu năng xử lý (performance)
- **`style`**     → Formatting code (chỉnh khoảng trắng, format linter, thiếu dấu chấm phẩy...)
- **`build`**     → Cấu hình các công cụ build (Docker, pom.xml...)
- **`ci`**        → Cấu hình các luồng CI/CD (GitHub Actions...)

---

## 3. Phạm Vi Thay Đổi (Scope)

Khai báo rõ commit của bạn tác động lên module hay khu vực nào của hệ thống. Với cấu trúc dự án hiện tại, các scope thường dùng gồm:

- `auth`, `chat`, `faq`, `ai`, `staff`, `guest`, `ticket`, `feed`, `integration`, `readme`, `progress`...

---

## 4. Một Số Ví Dụ Thực Tế Của Dự Án

Hãy tham khảo và áp dụng các mẫu commit dưới đây để History dự án luôn sạch sẽ:

- `feat(auth): add JWT authentication`
- `feat(chat): implement WebSocket STOMP messaging`
- `feat(faq): add semantic FAQ search`
- `feat(ai): integrate RAG pipeline`
- `feat(staff): add SLA dashboard`
- `feat(guest): add guest tracking`
- `fix(auth): resolve CSRF issue for API endpoints`
- `fix(chat): prevent duplicate WebSocket messages`
- `docs(readme): update project setup instructions`
- `docs(progress): update developer progress report`
- `test(faq): add FAQ matcher unit tests`
- `refactor(chat): separate WebSocket service from ticket service`

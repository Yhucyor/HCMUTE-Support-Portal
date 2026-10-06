# HƯỚNG DẪN QUY TRÌNH GIT: TÁCH NHÁNH FEATURE VÀ TÍCH HỢP VÀO MAIN

> **Chiến lược áp dụng**: *Feature Branch Workflow* (Mỗi chức năng làm việc trên 1 nhánh riêng biệt, kiểm thử hoàn tất mới gộp vào nhánh chính `master` / `main`).

---

## 1. MÔ HÌNH NHÁNH (GIT BRANCHING MODEL)

```
(main)    o-----------------------------------o (Merge Feat 1) -----o (Merge Feat 2) ---->
               \                             /                     /
(feature)       o--- feat/auth-rbac --------o                     /
                                             \                   /
(feature)                                     o--- feat/ticket -o
```

### Quy ước đặt tên nhánh (Branch Naming Convention):
- `feature/core-config`: Nền tảng, cấu hình, Common, Security
- `feature/auth-rbac`: Xác thực, phân quyền, Profile, OTP
- `feature/ticket-sla`: Quản lý vé hỗ trợ, SLA Engine, Staff Dashboard
- `feature/feed-moderation`: Bảng tin, Diễn đàn, Bình luận, Báo cáo vi phạm
- `feature/ai-rag-faq`: Chatbot AI, RAG Knowledge Hub, Tra cứu FAQ
- `feature/admin-internal`: Dashboard Admin, Nhắn tin nội bộ Admin-Staff, Webhook
- `feature/ui-thymeleaf`: Toàn bộ Giao diện Thymeleaf, CSS, JS, Asset hoàn chỉnh

---

## 2. QUY TRÌNH 5 BƯỚC THỰC HIỆN CHO MỖI CHỨC NĂNG

### 🔹 Bước 1: Cập nhật nhánh chính và tạo nhánh mới từ nhánh chính
Mở terminal tại thư mục dự án đích (`HCMUTE-Support-Portal`):
```bash
# 1. Chuyển về nhánh chính (master hoặc main)
git checkout master

# 2. Kéo code mới nhất về
git pull origin master

# 3. Tạo và chuyển sang nhánh tính năng mới
git checkout -b feature/auth-rbac
```

---

### 🔹 Bước 2: Chuyển các file thuộc tính năng đó & Kiểm tra
1. Chép các file thuộc tính năng đó từ `doancuoiki_demo1` sang `HCMUTE-Support-Portal` theo danh mục tại file [02_LO_TRINH_CHUYEN_GIAO_THEO_CHUC_NANG.md](file:///d:/HK5/LapTrinhWeb/doancuoiki/HCMUTE-Support-Portal/docs/chuyengiao/02_LO_TRINH_CHUYEN_GIAO_THEO_CHUC_NANG.md).
2. Kiểm tra trạng thái các file đã thay đổi:
```bash
git status
```
3. Chạy thử nghiệm để kiểm tra lỗi compile:
```bash
mvn clean compile
```

---

### 🔹 Bước 3: Đưa file vào Staging và Commit với thông điệp chuẩn (Conventional Commit)
```bash
# Thêm các file đã chỉnh sửa
git add .

# Commit với thông điệp rõ ràng
git commit -m "feat(auth): integrate authentication, rbac roles, otp and profile management"
```

*Quy ước tiền tố commit hữu ích:*
- `feat`: Thêm tính năng mới (ví dụ: `feat(ticket): add sla countdown engine`)
- `fix`: Sửa lỗi (ví dụ: `fix(auth): fix token expiration issue`)
- `docs`: Cập nhật tài liệu (ví dụ: `docs(db): add 17 tables analysis`)
- `style`: Sửa CSS, giao diện mà không đổi logic code

---

### 🔹 Bước 4: Kiểm thử tính năng độc lập (Feature Testing)
Chạy ứng dụng:
```bash
mvn spring-boot:run
```
Truy cập trình duyệt để xác nhận chức năng hoạt động chính xác theo yêu cầu:
- Ví dụ nhánh `feature/auth-rbac`: Đăng ký tài khoản mới, nhận OTP, đăng nhập các quyền Student / Staff / Admin.

---

### 🔹 Bước 5: Hợp nhất (Merge) nhánh Feature vào nhánh chính `master` / `main`
Khi tính năng đã hoạt động ổn định và không còn lỗi:
```bash
# 1. Chuyển lại về nhánh chính
git checkout master

# 2. Đảm bảo nhánh chính được cập nhật
git pull origin master

# 3. Thực hiện merge có cờ --no-ff (để lưu lại dấu vết nhánh tính năng trong lịch sử Git)
git merge --no-ff feature/auth-rbac -m "merge: integrate feature/auth-rbac into master"

# 4. Đẩy kết quả lên Github/Git Server từ xa
git push origin master

# 5. (Tùy chọn) Xóa nhánh feature cục bộ sau khi đã merge thành công
git branch -d feature/auth-rbac
```

---

## 3. KỊCH BẢN XỬ LÝ KHI XẢY RA XUNG ĐỘT (MERGE CONFLICT)

Nếu trong quá trình `git merge` xuất hiện thông báo:
`CONFLICT (content): Merge conflict in src/.../File.java`

Bạn xử lý theo 4 bước sau:
1. Mở file bị xung đột lên, tìm các đoạn đánh dấu:
   ```java
   <<<<<<< HEAD
   // Code đang có trên nhánh master
   =======
   // Code mới từ nhánh feature
   >>>>>>> feature/auth-rbac
   ```
2. Giữ lại đoạn code đúng và xoá các ký tự `<<<<<<<`, `=======`, `>>>>>>>`.
3. Kiểm tra lại và lưu file.
4. Chạy lệnh hoàn tất merge:
   ```bash
   git add .
   git commit -m "fix(merge): resolve merge conflict in File.java"
   git push origin master
   ```

---

## 4. TỔNG HỢP CÁC LỆNH GIT THƯỜNG DÙNG

| Tác vụ | Lệnh Git |
|:---|:---|
| Xem nhánh hiện tại | `git branch` |
| Tạo và nhảy sang nhánh mới | `git checkout -b feature/<ten_nhanh>` |
| Xem các file thay đổi | `git status` |
| Xem chi tiết từng dòng thay đổi | `git diff` |
| Lưu tạm thay đổi khi cần chuyển nhánh gấp | `git stash` |
| Lấy lại thay đổi đã lưu tạm | `git stash pop` |
| Xem lịch sử commit dạng sơ đồ đẹp | `git log --graph --oneline --all` |
| Hủy bỏ thay đổi chưa commit của 1 file | `git checkout -- <duong_dan_file>` |

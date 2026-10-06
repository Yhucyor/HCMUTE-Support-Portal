# HCMUTE Student Portal - Development Plan & Git Workflow

Tài liệu này mô tả kế hoạch phát triển chi tiết cho dự án HCMUTE Student Portal, bao gồm các tính năng cần bổ sung và quy trình quản lý mã nguồn (Git Workflow) chuyên nghiệp.

## 🌳 Git Workflow (Quy trình quản lý nhánh)

Dự án áp dụng mô hình Git Flow cơ bản với 3 loại nhánh chính:

1.  **`main`**: Nhánh Production. Code ở đây luôn phải ở trạng thái ổn định và sẵn sàng chạy (deploy). **KHÔNG** code trực tiếp trên nhánh này.
2.  **`develop`**: Nhánh Development. Dùng để tích hợp các tính năng mới trước khi đưa lên `main`.
3.  **`feature/<tên-tính-năng>`**: Nhánh tính năng. Được tạo ra từ nhánh `develop`. Sau khi hoàn thành, sẽ được merge ngược lại vào `develop`.

### Chu trình làm việc chuẩn cho mỗi tính năng:
```bash
# 1. Đảm bảo đang ở nhánh develop và cập nhật mới nhất
git checkout develop
git pull origin develop

# 2. Tạo nhánh feature mới từ develop
git checkout -b feature/tên-tính-năng

# 3. Code, commit trên nhánh feature
git add .
git commit -m "feat(ui): mô tả công việc vừa làm (ví dụ: add timetable page)"

# 4. Khi hoàn thành, merge vào develop
git checkout develop
git merge feature/tên-tính-năng
git branch -d feature/tên-tính-năng # (Tùy chọn) Xóa nhánh feature sau khi merge xong

# 5. Đẩy code lên server
git push origin develop
```

*Lưu ý: Sau khi hoàn thành trọn vẹn một **Giai đoạn (Phase)** trên nhánh `develop` và test kỹ lưỡng, mới tiến hành merge `develop` vào `main`.*

---

## 🚀 Kế hoạch phát triển từng giai đoạn (Implementation Plan)

### Giai đoạn 1: Chuẩn hóa Layout và System Pages
**Mục tiêu:** Tạo bộ khung dùng chung (Header, Footer, Sidebar) để dễ dàng thêm trang mới mà không bị lặp code.

*   **Nhánh:** `feature/layout-standardization`
*   **Công việc:**
    *   Tạo `fragments/header.html`, `fragments/footer.html`, `fragments/sidebar.html`.
    *   Tạo `layout.html` làm template gốc.
    *   Cập nhật các trang hiện tại (`index.html`, `login.html`, `dashboard.html`...) sử dụng layout mới.
    *   Tạo trang cài đặt (`settings.html`).
    *   Tạo trang lỗi (`error-404.html`, `error-500.html`).
*   **Merge:** `feature/layout-standardization` ➔ `develop`
*   **Release:** Merge `develop` ➔ `main` (Release v1.1)

### Giai đoạn 2: Phát triển Core Student Features (Học tập)
**Mục tiêu:** Cung cấp các công cụ cốt lõi sinh viên cần xem hàng ngày.

*   **Nhánh:** `feature/academic-pages`
*   **Công việc:**
    *   Tạo `timetable.html`: Giao diện thời khóa biểu, lịch thi.
    *   Tạo `grades.html`: Giao diện bảng điểm, biểu đồ thống kê GPA tích lũy.
    *   Tạo `curriculum.html`: Chương trình đào tạo chuẩn của sinh viên.
*   **Merge:** `feature/academic-pages` ➔ `develop`
*   **Release:** Merge `develop` ➔ `main` (Release v1.2)

### Giai đoạn 3: Phát triển nhóm Tài chính & Dịch vụ (Services)
**Mục tiêu:** Hỗ trợ sinh viên thanh toán học phí và yêu cầu giấy tờ hành chính.

*   **Nhánh:** `feature/financial-services`
*   **Công việc:**
    *   Tạo `tuition.html`: Giao diện theo dõi học phí, công nợ, lịch sử giao dịch.
    *   Tạo `document-request.html`: Form xin cấp giấy tờ (bảng điểm, giấy xác nhận sinh viên).
    *   Tạo `scholarships-dormitory.html`: Thông tin nội trú và học bổng.
*   **Merge:** `feature/financial-services` ➔ `develop`
*   **Release:** Merge `develop` ➔ `main` (Release v1.3)

### Giai đoạn 4: Hoàn thiện dữ liệu (Mock data) & Tương tác
**Mục tiêu:** Làm sống động UI bằng dữ liệu từ Backend, xử lý các form submit.

*   **Nhánh:** `feature/dynamic-data-binding`
*   **Công việc:**
    *   Đổ dữ liệu từ Controller xuống các trang (Thymeleaf Model).
    *   Xử lý logic validation cho các form.
    *   Xử lý tương tác chuyển trang, thông báo toast (success/error).
*   **Merge:** `feature/dynamic-data-binding` ➔ `develop`
*   **Release:** Merge `develop` ➔ `main` (Release v2.0 - Hoàn thiện UI/UX)

---

## 🛠 Cách bắt đầu dự án ngay bây giờ (Dành cho Developer)

Khởi tạo các nhánh cần thiết nếu repo của bạn chưa có:
```bash
# Đảm bảo bạn đang ở nhánh main (hoặc master)
git checkout main

# Tạo nhánh develop từ main
git checkout -b develop
git push -u origin develop

# Bắt đầu Giai đoạn 1
git checkout -b feature/layout-standardization
```

# 🎓 QAUTE Portal - HCMUTE Student Support & Counseling Portal

> **Hệ thống Tư vấn Sinh viên, Quản lý Ticket SLA Đa Phòng Ban, Bảng Tin Đa Phương Tiện tích hợp Trợ Lý AI RAG & Microservice Video**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Spring%20Security-6.x-blue.svg)](https://spring.io/projects/spring-security)
[![Database](https://img.shields.io/badge/Database-MySQL%20%7C%20SQL%20Server-blue.svg)](https://www.mysql.com/)
[![E2E Testing](https://img.shields.io/badge/Playwright-E2E%20Tested-green.svg)](https://playwright.dev/)

---

## 📌 1. Giới Thiệu Dự Án

**QAUTE Portal** là cổng thông tin và hỗ trợ học vụ toàn diện dành cho sinh viên trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE), giải quyết các bài toán trọng tâm:
* 🤖 **Trợ lý AI RAG 24/7:** Đọc hiểu văn bản quy chế và hơn 2.600+ câu hỏi thực tế để trả lời sinh viên tức thì, tự động gợi ý chuyển thành Ticket khi cần cán bộ can thiệp.
* 🎫 **Động cơ Quản lý Ticket & SLA Deadline:** Phân luồng theo Khoa/Phòng (Tuyển sinh, Đào tạo, Đoàn trường, Khoa CNTT, Ngoại ngữ...), tự động tính hạn chót xử lý (`URGENT: 24h`, `MEDIUM: 72h`, `LOW: 7d`).
* 📰 **Cổng Thông Tin & Diễn Đàn 2 Luồng:**
  * *Bảng tin chính thức:* Cán bộ đăng thông báo đính kèm file PDF, Word, Excel và Video MP4.
  * *Diễn đàn sinh viên:* Giao diện dạng Feed có kiểm duyệt bài viết (`PENDING_APPROVAL` $\rightarrow$ `APPROVED`), Like, Comment và Trung tâm Báo cáo vi phạm (Report).
* 🎥 **Tích hợp Webhook Video:** Tiếp nhận video render tự động từ Microservice Node.js qua xác thực chữ ký HMAC-SHA256.

---

## 🏛️ 2. Kiến Trúc & Công Nghệ Sử Dụng

* **Ngôn ngữ & Nền tảng:** Java 17, Spring Boot 3.3.x, Jakarta Servlet API 6.0 (`jakarta.servlet.*`).
* **Bảo mật & Phân quyền:** Spring Security 6, Session Cookie `JSESSIONID` (`HttpOnly`, `SameSite=Lax`, CSRF), JWT Token Provider, Phân quyền chống can thiệp chéo Khoa (`@deptSecurity`).
* **Cơ sở dữ liệu & ORM:** Hibernate ORM 6.5.2 (JPA 3.1), Hỗ trợ cả **MySQL 8** và **Microsoft SQL Server**.
* **Giao diện (Frontend):** Thymeleaf Template Engine, Bootstrap 5 (Academic Minimalist style).
* **Lưu trữ tệp (Storage):** Hỗ trợ Dual-mode (Cloudinary Cloud Storage & Local Folder `Constant.DIR = C:\upload`).
* **Kiểm thử tự động:** Playwright E2E Automation Testing.

---

## 📂 3. Cấu Trúc Thư Mục Modular Monolith

```text
com.school.counseling
  ├── common/               # BaseEntity, ExceptionHandler, StorageService, Constant
  ├── config/               # SecurityConfig, AsyncConfig, StorageConfig, WebMvcConfig
  └── module/
        ├── auth/           # Xác thực, OTP Email, Phân quyền RBAC, User & Department
        ├── ticket/         # Ticket Lifecycle, SLA Engine, Messages, Guest Access Token
        ├── chat/           # Conversation, Message, Attachment
        ├── feed/           # Bảng tin chính thức, Diễn đàn, Duyệt bài, Báo cáo vi phạm
        ├── integration/    # Webhook bảo mật tiếp nhận Video MP4 từ Node.js
        └── ai/             # RAG Engine, Knowledge Store, Tra cứu 2.600+ Q&A thực tế
```

---

## 🚀 4. Hướng Dẫn Cài Đặt & Khởi Chạy

### 4.1. Yêu Cầu Môi Trường
* JDK 17 trở lên.
* Maven 3.8+.
* MySQL 8.0+ (hoặc Microsoft SQL Server).
* Spring Tool Suite (STS) / IntelliJ IDEA / Antigravity IDE.

### 4.2. Các Bước Chạy Ứng Dụng
```bash
# 1. Clone repository
git clone git@github.com:cuong31126/hcmute-student-support-portal.git
cd hcmute-student-support-portal

# 2. Tạo database MySQL
mysql -u root -p -e "CREATE DATABASE qaute_portal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. Import Schema & DDL
mysql -u root -p qaute_portal < docs/database/srs_schema.sql

# 4. Chạy ứng dụng Spring Boot
mvn spring-boot:run
```
* Ứng dụng sẽ khởi chạy tại: `http://localhost:8080`

### 4.3. Chạy Bộ Kiểm Thử E2E Playwright
```bash
cd tests/e2e
npm install
npx playwright test
```

---

## 👥 5. Phân Quyền Tài Khoản Mẫu

| Tài khoản | Mật khẩu | Vai trò (Role) | Phạm vi quản lý |
| :--- | :--- | :--- | :--- |
| `admin` | `Password123@` | `ROLE_ADMIN` | Toàn quyền hệ thống |
| `staff_tuyensinh` | `Password123@` | `ROLE_STAFF` | Phòng Tuyển sinh & Truyền thông |
| `staff_daotao` | `Password123@` | `ROLE_STAFF` | Phòng Đào tạo & Công tác Sinh viên |
| `staff_cntt` | `Password123@` | `ROLE_STAFF` | Khoa Công nghệ Thông tin |
| `student01` | `Password123@` | `ROLE_STUDENT` | Sinh viên chính quy |

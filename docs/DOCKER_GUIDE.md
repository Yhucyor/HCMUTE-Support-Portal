# 🐳 Hướng Dẫn Sử Dụng Docker Cho Dự Án QAUTE Portal

Tài liệu này hướng dẫn cách chạy toàn bộ hệ thống (Ứng dụng Spring Boot + Database MySQL) thông qua Docker một cách độc lập, an toàn và không gây xung đột với các phần mềm có sẵn trên máy tính của bạn.

---

## 1. Kiến Trúc Khởi Chạy
Dự án sử dụng `docker-compose.yml` để khởi chạy đồng thời 2 thành phần (container):
- **Cơ sở dữ liệu (MySQL 8.0):** Chạy ngầm, cấu hình tự động lưu trữ dữ liệu (persistent volume) để không bị mất khi tắt.
- **Back-end (Spring Boot):** Tự động build ra file `.jar` thông qua Maven Image và chạy trên môi trường Java 17 siêu nhẹ.

### 🌐 Ánh Xạ Cổng (Port Mapping) - Tránh Xung Đột
Để không bị lỗi đụng độ port với máy tính thật (Host) của bạn:
- **Ứng dụng Spring Boot:** Chạy bên trong port `8080`, nhưng ánh xạ ra ngoài port `8088`. 
  👉 Truy cập web tại: `http://localhost:8088`
- **Cơ sở dữ liệu MySQL:** Chạy bên trong port `3306`, nhưng ánh xạ ra ngoài port `3307`. 
  👉 Bạn có thể dùng DBeaver / DataGrip / Navicat để kết nối với port `3307` từ bên ngoài.

---

## 2. Các Lệnh Thao Tác Thường Dùng

Mở Terminal (Command Prompt / PowerShell / Git Bash) tại **thư mục gốc của dự án** (nơi chứa file `docker-compose.yml`) và sử dụng các lệnh sau:

### 🚀 Khởi chạy hệ thống (Run)
```bash
# Lệnh này sẽ build code mới nhất và chạy ngầm (background) cả 2 container
docker-compose up -d --build
```
*Mẹo: Lần đầu tiên chạy lệnh này có thể mất vài phút vì Docker cần tải các hệ điều hành và thư viện Maven.*

### 🛑 Tắt hệ thống (Stop)
```bash
# Lệnh này sẽ dừng và xóa các container tạm thời (dữ liệu MySQL VẪN được giữ lại)
docker-compose down
```

### 📋 Xem Log (Nhật ký lỗi)
Nếu web không chạy được, bạn cần xem log của ứng dụng Spring Boot:
```bash
# Xem log trực tiếp của cả app và database (Ctrl+C để thoát)
docker-compose logs -f

# Hoặc chỉ xem log của app Spring Boot
docker-compose logs -f app
```

### 🧹 Xóa Toàn Bộ (Bao gồm Data)
Nếu bạn muốn Reset hoàn toàn (xóa luôn dữ liệu MySQL cũ để làm lại từ đầu):
```bash
docker-compose down -v
```

---

## 3. Khắc Phục Sự Cố Thường Gặp (Troubleshooting)

### Lỗi 1: Port is already allocated
- **Nguyên nhân:** Cổng `8088` hoặc `3307` trên máy bạn đã có phần mềm khác chiếm dụng.
- **Cách khắc phục:** Mở file `docker-compose.yml` và đổi cổng ánh xạ bên ngoài (số bên trái). 
  Ví dụ: Sửa `- "8088:8080"` thành `- "8089:8080"`.

### Lỗi 2: Không thấy code mới được cập nhật
- **Nguyên nhân:** Bạn vừa sửa code Java nhưng quên gắn flag `--build`. Docker vẫn đang chạy bản build cũ.
- **Cách khắc phục:** Tắt đi và build lại:
  ```bash
  docker-compose down
  docker-compose up -d --build
  ```

### Lỗi 3: Kết nối Database thất bại (Access Denied)
- **Thông tin đăng nhập mặc định (theo `docker-compose.yml`):**
  - Database URL (ngoài Docker): `jdbc:mysql://localhost:3307/qaute_portal`
  - Username: `qaute_user`
  - Password: `qautepassword`

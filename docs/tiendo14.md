# 📋 BÁO CÁO TIẾN ĐỘ & PHƯƠNG ÁN TỐI ƯU HẠ TẦNG: GIAI ĐOẠN 14 (TIENDO 14)
**Dự án:** Cổng thông tin hỗ trợ & tư vấn học vụ HCMUTE (HCMUTE Support Portal - QAUTE)  
**Tác giả:** Dev A (Fullstack & Software Architect) & Dev B (AI & Data Specialist)  
**Thời gian cập nhật:** 26/10/2026  
**Chủ đề:** Tích hợp DBeaver với MySQL Workbench có sẵn & Giải pháp tối ưu cài đặt Ollama Local khi ổ C còn 30GB

---

## 🎯 1. KẾT NỐI DBEAVER VỚI DATABASE HIỆN CÓ TỪ MYSQL WORKBENCH

### 1.1. Hiện trạng cơ sở dữ liệu trên máy
* Máy tính hiện **đã cài đặt sẵn MySQL Workbench** và toàn bộ cơ sở dữ liệu học vụ (`qaute_portal`), cấu trúc 17 bảng, khóa ngoại, tài khoản người dùng, ticket và quy định đào tạo **đều đã tồn tại đầy đủ**.
* **Nguyên tắc vận hành:** Tuyệt đối **không cần tạo lại bảng hay chạy lại file SQL**. Cả MySQL Workbench lẫn DBeaver thực chất chỉ là 2 "giao diện người dùng" (Client GUI) cùng kết nối vào một Service chạy ngầm duy nhất là **MySQL Server Service** tại cổng `3306`.

```mermaid
graph TD
    subgraph Clients["Trình quản lý Database (Client GUI)"]
        WB["MySQL Workbench (Đã có sẵn)"]
        DB["DBeaver Community (Mới cài đặt)"]
    end
    WB -->|Port 3306| SVR[("MySQL Server Service (localhost:3306)")]
    DB -->|Port 3306| SVR
    SVR --> DATA[("Database: qaute_portal<br/>(17 bảng, toàn bộ data giữ nguyên)")]
    DB -.->|Port 4000 (SSL)| TIDB[("TiDB Cloud Serverless (test)")]
```

### 1.2. Các bước mở dữ liệu ngay lập tức trên DBeaver
1. Mở DBeaver vừa cài đặt.
2. Bấm vào biểu tượng **New Database Connection** (phích cắm màu xanh góc trái trên cùng) $\rightarrow$ Chọn **MySQL**.
3. Nhập đúng thông số mà MySQL Workbench đang sử dụng:
   * **Host:** `localhost`
   * **Port:** `3306`
   * **Database:** `qaute_portal` (hoặc tên database hiện tại trên Workbench)
   * **Username:** `root`
   * **Password:** `caucuong` (hoặc mật khẩu bạn đang dùng cho Workbench)
4. Bấm **Test Connection** $\rightarrow$ Báo kết nối thành công $\rightarrow$ Bấm **Finish**.
5. Mở cây thư mục bên trái: Toàn bộ bảng dữ liệu, triggers, views từ Workbench sẽ hiển thị đầy đủ 100% trong DBeaver mà không cần cấu hình thêm gì.

---

## 💻 2. KHẢO SÁT & ĐÁNH GIÁ PHẦN CỨNG THỰC TẾ CỦA MÁY

Qua kiểm tra trực tiếp thông số hệ thống phần cứng:

| Thành phần phần cứng | Thông số chi tiết thực tế | Đánh giá năng lực xử lý AI |
| :--- | :--- | :--- |
| **CPU** | **Intel(R) Core(TM) i7-1255U** (10 nhân, 12 luồng, Turbo 4.7GHz) | CPU thế hệ 12 rất mạnh, hỗ trợ tập lệnh AVX2, xử lý vector lượng tử hóa tốt. |
| **RAM** | **16.0 GB** | Dư dả cho các tác vụ thông thường, nhưng cần phân bổ: Windows + IDE + Chrome + Spring Boot ngốn ~8-10GB. |
| **GPU** | **Intel(R) Iris(R) Xe Graphics** (GPU Onboard) | Không có VRAM rời (như NVIDIA RTX), model AI sẽ chạy chủ yếu bằng CPU + chia sẻ RAM. |
| **Ổ đĩa C** | **30.14 GB còn trống** (trên 256 GB) | ⚠️ **Mức cảnh báo nhạy cảm!** Tránh tối đa việc ghi file lớn vào ổ C. |
| **Ổ đĩa D** | **58.66 GB còn trống** (trên 196 GB) | 🟢 **Thoải mái hơn nhiều**, là nơi lý tưởng để lưu trữ các model AI nặng hàng Gigabyte. |

---

## 🛡️ 3. NGUY CƠ KHI CÀI OLLAMA MẶC ĐỊNH & CHIẾN LƯỢC BẢO VỆ Ổ C (30GB)

### 3.1. Nguy cơ tràn ổ C nếu cài đặt mặc định
* Theo thiết kế mặc định của Ollama trên Windows:
  1. Ứng dụng Ollama cài vào: `C:\Users\<User>\AppData\Local\Programs\Ollama`
  2. **ĐIỂM NGUY HIỂM:** Toàn bộ model AI khi gõ lệnh `ollama run ...` sẽ được tải ngầm vào thư mục:  
     `C:\Users\<User>\.ollama\models`
  3. Nếu tải thử 2-3 model tầm trung (ví dụ model 7B khoảng 4.8GB - 5.5GB), kèm theo bộ nhớ đệm ngữ cảnh (context cache), ổ C 30GB sẽ nhanh chóng tụt xuống dưới 10GB. Lúc này Windows sẽ liên tục báo cảnh báo Low Disk Space, máy bị đơ giật và không thể cập nhật phần mềm.

---

### 3.2. Giải pháp tối ưu số 1: Di chuyển toàn bộ Model Ollama sang ổ D

Để bảo toàn tuyệt đối 30GB quý giá của ổ C, ta thiết lập biến môi trường hệ thống để Ollama **tự động lưu 100% dữ liệu model sang ổ D**:

```mermaid
graph LR
    A["Lệnh: ollama run qwen2.5:1.5b"] --> B["Ollama Engine (Windows Service)"]
    B -->|Đọc biến OLLAMA_MODELS| C{"Thư mục lưu trữ"}
    C -->|Mặc định (NGUY HIỂM)| D["Ổ C: Users/Quoc Cuong/.ollama/models<br/>(Làm cạn 30GB ổ C)"]
    C -->|Đã cấu hình tối ưu| E["Ổ D: D:/ollama_models<br/>(Bảo toàn ổ C an toàn 100%)"]
```

#### Các bước thiết lập thư mục lưu trữ Model sang ổ D:
1. **Tạo thư mục trên ổ D:**
   Mở PowerShell chạy lệnh:
   ```powershell
   New-Item -ItemType Directory -Force -Path "D:\ollama_models"
   ```
2. **Cấu hình biến môi trường vĩnh viễn (User Environment Variable):**
   Chạy lệnh PowerShell sau (chỉ cần chạy 1 lần duy nhất):
   ```powershell
   [Environment]::SetEnvironmentVariable("OLLAMA_MODELS", "D:\ollama_models", "User")
   ```
3. Sau khi đặt biến này, bất kỳ model nào được kéo về (`pull` hoặc `run`) đều sẽ nằm trọn trong `D:\ollama_models`, **ổ C không bị tốn một megabyte nào cho model!**

---

## 🧠 4. BẢNG SO SÁNH VÀ LỰA CHỌN MODEL TỐI ƯU NHẤT CHO PHẦN CỨNG

Với CPU **Intel Core i7-1255U** và **16GB RAM (không GPU rời)**, việc chọn model phải cân đối giữa **Tốc độ (Speed)**, **Tiêu tốn tài nguyên (Footprint)** và **Khả năng hiểu tiếng Việt (Vietnamese Capability)**:

| Tiêu chí | 🌟 Qwen 2.5 - 1.5B (Khuyên dùng #1) | Qwen 2.5 - 3B (Lựa chọn #2) | Llama 3.2 - 3B | Qwen 2.5 - 7B (Không khuyến nghị) |
| :--- | :---: | :---: | :---: | :---: |
| **Dung lượng tải (Disk)** | **~1.0 GB** | **~2.0 GB** | ~2.0 GB | ~4.7 GB |
| **Chiếm dụng RAM khi chạy** | **~1.5 GB - 1.8 GB** | **~2.8 GB - 3.2 GB** | ~3.0 GB | ~6.5 GB - 8.0 GB (Rất nặng) |
| **Tốc độ sinh text (i7-1255U)** | ⚡ **30 - 45 tokens/s** (Siêu mượt) | ⚡ **18 - 25 tokens/s** (Nhanh) | 15 - 20 tokens/s | 🐌 4 - 7 tokens/s (Chờ lâu) |
| **Khả năng hiểu tiếng Việt** | ⭐⭐⭐⭐⭐ (Xuất sắc) | ⭐⭐⭐⭐⭐ (Rất sâu) | ⭐⭐⭐ (Trung bình) | ⭐⭐⭐⭐⭐ (Sâu nhưng chậm) |
| **Khả năng chạy cùng IDE + Web** | 🟢 Cực kỳ nhẹ nhàng, máy mát | 🟢 Ổn định, mượt mà | 🟡 Hơi tốn RAM | 🔴 Quạt quay to, lag máy |

> [!TIP]
> **Đề xuất tối ưu tuyệt đối cho đồ án:**
> * Hãy cài đặt model **`qwen2.5:1.5b`** (hoặc bản nâng cao **`qwen2.5:3b`**).
> * Qwen 2.5 được cộng đồng AI đánh giá là mô hình mã nguồn mở hỗ trợ **tiếng Việt xuất sắc nhất hiện nay trong phân khúc dưới 3B tham số**, đọc hiểu các văn bản quy định, học vụ, điều khoản đào tạo của HCMUTE cực kỳ chuẩn xác và không hề bị ngọng ngữ pháp tiếng Việt.

---

## 🚀 5. HƯỚNG DẪN CÀI ĐẶT & KHỞI CHẠY OLLAMA TỐI ƯU (3 BƯỚC)

### Bước 1: Cài đặt Ollama vào máy
1. Truy cập trang chủ: [https://ollama.com/download](https://ollama.com/download) $\rightarrow$ Tải file `OllamaSetup.exe` (chỉ khoảng 50MB).
2. Tiến hành cài đặt (bấm `Install`). Ứng dụng sẽ tự động khởi chạy icon con lạc đà ở khay hệ thống (System Tray).

### Bước 2: Đảm bảo biến môi trường trỏ vào ổ D
Tắt Ollama ở khay hệ thống (chuột phải icon $\rightarrow$ Quit), sau đó mở PowerShell chạy lệnh:
```powershell
[Environment]::SetEnvironmentVariable("OLLAMA_MODELS", "D:\ollama_models", "User")
```
Sau đó mở lại Ollama từ Start Menu để nhận đường dẫn mới.

### Bước 3: Tải và chạy thử nghiệm model nhẹ nhàng
Mở Terminal / PowerShell và gõ lệnh:
```powershell
ollama run qwen2.5:1.5b
```
* Ollama sẽ tự động tải file model (~1GB) về thư mục `D:\ollama_models`.
* Gõ thử câu hỏi tiếng Việt: `Chào bạn, quy chế học vụ HCMUTE có những điểm gì cần lưu ý?`
* Bạn sẽ thấy câu trả lời tuôn ra với tốc độ tức thời và cực kỳ thông minh.

---

## 🔗 6. KẾT NỐI VỚI HỆ THỐNG QAUTE PORTAL (SPRING BOOT)

1. Giao diện [`ai-chat.html`](file:///d:/HK5/LapTrinhWeb/doancuoiki\ban2\src\main\resources\templates\ai-chat.html) trên nhánh `dev` đã tích hợp sẵn mục chọn:  
   👉 **`Ollama Local (Qwen 2.5)`**
2. Cổng mặc định của Ollama là: `http://localhost:11434`.
3. Khi sinh viên bấm chat trên web với model này, hệ thống sẽ gọi trực tiếp vào Ollama đang chạy trên máy mà không tốn bất kỳ chi phí API hay cần kết nối Internet ra bên ngoài.

---

## 📌 7. TỔNG KẾT HÀNH ĐỘNG CẦN LÀM
1. ✅ **DBeaver:** Đang được tải về máy; khi mở lên chỉ cần kết nối vào database `qaute_portal` sẵn có của MySQL Workbench.
2. ✅ **Ổ C (30GB):** Được bảo vệ an toàn 100% bằng cách điều hướng `OLLAMA_MODELS` sang ổ D (`D:\ollama_models`).
3. ✅ **Model khuyên dùng:** Chạy duy nhất `qwen2.5:1.5b` (1.0GB) hoặc `qwen2.5:3b` (2.0GB) để đạt hiệu năng cao nhất trên chip Intel i7-1255U.

# 🔍 CHUYỆN KỂ CHO HỌC SINH LỚP 5: VÌ SAO CÁC TICKET "TÀNG HÌNH" TRÊN BÀN CÁN BỘ?

> **Tài liệu phân tích sự cố kỹ thuật:** Spring Data JPA / Hibernate 6 Implicit INNER JOIN & Role Admin Scope Fallback  
> **Người viết:** Senior Spring Boot Architect  
> **Ngôn ngữ giải thích:** Phong cách ngụ ngôn siêu đơn giản, hình tượng hóa cho học sinh lớp 5 dễ hiểu 🧒🎒  

---

## ⚡ TÓM TẮT SIÊU NHANH TRONG 3 DÒNG

1. **Lỗi 1 (Chiếc vé tàng hình):** Lập trình viên vô tình gọi tên "Bác tài xế" trên những chiếc xe buýt chưa có tài xế, làm chú bảo vệ Database tưởng xe hỏng nên giấu sạch toàn bộ xe đi (trả về 0 ticket).
2. **Lỗi 2 (Thầy Hiệu Trưởng vào nhầm lớp):** Thầy Hiệu Trưởng quản lý toàn trường nhưng hệ thống lại dắt Thầy vào nhầm phòng thể dục trống trơn, trong khi đơn từ của học sinh đang nằm ở phòng thư viện.
3. **Kết quả sau khi sửa:** Cán bộ Phòng Đào tạo và Thầy Hiệu Trưởng đã nhìn thấy đầy đủ 100% các Ticket đang mở, giao diện nhắn tin trao đổi đẹp như Messenger!

---

## 🚌 CÂU CHUYỆN 1: BÁC BẢO VỆ DATABASE VÀ CHIẾC XE BUÝT CHƯA CÓ TÀI XẾ

### 1. Chiếc xe buýt trường học (Vé Ticket)
* Mỗi khi một bạn học sinh gặp khó khăn và gửi một câu hỏi lên trường, hệ thống sẽ tạo ra một **chiếc xe buýt chở câu hỏi** (gọi là Ticket).
* Xe vừa mới xuất phát từ bến thì đang ở trạng thái **Chờ tiếp nhận (`OPEN`)**.
* Vì xe mới tinh nên **chưa có Thầy/Cô nào nhận lái** $\rightarrow$ Ghế lái xe đang để trống (`assigned_to = NULL`).

### 2. Bác bảo vệ nghiêm khắc (Cơ chế JPA / Hibernate)
Trước đây, trong sổ kiểm tra xe (câu lệnh truy vấn JPQL), lập trình viên viết một dòng chữ:
> *"Hãy đón tất cả các xe buýt của Phòng Đào tạo, và đọc to họ tên của Bác tài xế đang lái xe (`t.assignedTo.fullName`)!"*

Bác bảo vệ Database đọc xong quy định này liền tự suy luận:
* "Ủa? Quy định bảo đọc tên Bác tài xế, mà mấy chiếc xe này ghế lái đang trống trơn, làm gì có ai lái?"
* Bác bảo vệ liền **đuổi sạch tất cả các xe buýt chưa có tài xế quay đầu lại**, không cho chiếc nào đi qua cổng cả!
* Trong nghề lập trình, hành động "bắt buộc phải có tài xế mới cho qua" này gọi là **`INNER JOIN` (Phép nối bắt buộc)**.

### 3. Hậu quả
Vì tất cả câu hỏi học sinh mới gửi đều chưa kịp có Thầy/Cô nào nhận, nên 100% câu hỏi đều chưa có tài xế. Bác bảo vệ đuổi hết sạch, khiến Thầy/Cô trực ban mở cổng ra **không thấy một chiếc xe nào (0 ticket)**!

```
[Học sinh gửi xe buýt mới] (Chưa có tài xế: NULL)
           │
           ▼
[Cổng bảo vệ JPA: t.assignedTo.fullName]
           │
           ├─ Bác bảo vệ: "Không có tài xế à? ĐUỔI HẾT!" (INNER JOIN ngầm)
           ▼
[Bàn Cán bộ]: TRỐNG TRƠN (0 TICKET) 😱
```

### 4. Bác bảo vệ được dặn lại thế nào? (Cách sửa)
Lập trình viên sửa lại lời dặn bằng câu thần chú **`LEFT JOIN`**:
> *"Bác bảo vệ ơi, hãy cho tất cả xe buýt đi qua cổng nhé! Xe nào có Thầy/Cô lái rồi thì đọc tên Thầy/Cô, còn xe nào chưa có ai lái thì cứ ghi là 'Chưa có', tuyệt đối không được đuổi xe đi!"*

Thế là xong! Toàn bộ các xe buýt câu hỏi của học sinh đã chạy bon bon vào bàn trực ban của Thầy/Cô.

---

## 🏫 CÂU CHUYỆN 2: THẦY HIỆU TRƯỞNG ĐI NHẦM PHÒNG HỌC

### 1. Thầy Hiệu Trưởng là ai? (`ROLE_ADMIN`)
* Cán bộ tư vấn (`ROLE_STAFF`) thì thuộc về một phòng cụ thể (ví dụ: Cô Lan thuộc Phòng Đào tạo `ID = 3`, Thầy Nam thuộc Khoa CNTT `ID = 4`).
* Nhưng Thầy Hiệu Trưởng (`Admin`) thì quản lý **toàn bộ ngôi trường**, Thầy không thuộc riêng một lớp hay một phòng nào cả (`department_id = NULL`).

### 2. Sự cố dắt nhầm phòng
Trước đây, hệ thống thấy Thầy Hiệu Trưởng không ghi số phòng, liền tự động đoán mò:
> *"À, không ghi phòng à? Thế dắt Thầy vào Phòng số 1 (Đoàn Thanh Niên) cho nhanh!"*

Khổ nỗi:
* Bạn học sinh thì đang gửi đơn xin cứu trợ học phần sang **Phòng Đào tạo (Phòng số 3)**.
* Thầy Hiệu Trưởng thì bị dắt vào ngồi ở **Phòng Đoàn Thanh Niên (Phòng số 1)**.
* Thầy mở ngăn bàn ra xem thì không thấy lá đơn nào của bạn học sinh cả!

### 3. Sửa lại quyền năng cho Thầy Hiệu Trưởng (`BRULE-TICKET-004`)
* **Mặc định:** Cho Thầy Hiệu Trưởng đứng từ đài quan sát nhìn **Toàn trường** $\rightarrow$ Thấy hết tất cả đơn từ của tất cả các Khoa/Phòng gom lại một chỗ.
* **Bộ lọc thông minh:** Nếu Thầy muốn xem riêng Phòng Đào tạo hay Khoa CNTT, Thầy chỉ cần bấm vào chiếc hộp chọn (dropdown) là hệ thống sẽ chuyển ngay tới phòng đó cho Thầy kiểm tra.

---

## 💬 CẢI TIẾN THÊM: GIAO DIỆN CHAT ĐẸP NHƯ MESSENGER

Thay vì hiển thị danh sách phản hồi khô khan bằng những dòng kẻ dọc màu xám đơn điệu:
1. **Bong bóng chat xịn xò:**
   - Tin nhắn của bạn: Nằm bên **phải**, nền màu xanh dương gradient lấp lánh giống hệt Messenger.
   - Tin nhắn của Thầy/Cô: Nằm bên **trái**, có avatar Thầy/Cô và huy hiệu *Tư Vấn Viên* màu tím trang trọng.
2. **Thông báo hệ thống đáng yêu:**
   - Khi bạn tạo yêu cầu, Thầy/Cô bấm nhận xử lý, hay Thầy/Cô đưa ra giải pháp chính thức $\rightarrow$ Đều hiện thành một chiếc huy hiệu viên thuốc xinh xắn nằm ngay ngắn ở giữa màn hình.
3. **Phím tắt tiện lợi:**
   - Bạn chỉ cần gõ tin nhắn rồi nhấn tổ hợp phím **Ctrl + Enter** là tin nhắn sẽ bay vèo tới Thầy/Cô ngay tức khắc!

---

## 📝 BÀI HỌC KHOA HỌC DÀNH CHO LẬP TRÌNH VIÊN NHÍ
1. **Đừng bao giờ bắt người ta gọi tên một thứ chưa tồn tại:** Trong cơ sở dữ liệu, nếu một mối liên kết có thể rỗng (`NULL`), phải luôn dùng `LEFT JOIN`, đừng bao giờ viết tắt chấm trực tiếp (`t.assignedTo.fullName`).
2. **Cẩn thận khi đoán mò giá trị mặc định:** Với người dùng cấp cao như Admin, không được tự ý ép họ vào một góc phòng chật hẹp, mà phải trao cho họ bức tranh toàn cảnh!

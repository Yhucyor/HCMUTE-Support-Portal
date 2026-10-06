# BÁO CÁO PHÂN TÍCH 17 BẢNG DATABASE VÀ ĐÁNH GIÁ 5 BẢNG BỔ TRỢ

> **Dự án**: Cổng Tư vấn & Hỗ trợ Sinh viên QAUTE Portal (HCMUTE Support Portal)  
> **Tech Stack**: Spring Boot 3, Spring Data JPA, MySQL 8, Thymeleaf, Python AI RAG  

---

## 1. TỔNG QUAN 17 BẢNG TRONG DATABASE CỦA `doancuoiki_demo1`

Hiện tại hệ thống `doancuoiki_demo1` có **17 Entity JPA** tương ứng với **17 bảng** trong MySQL:

| STT | Tên Bảng (Table Name) | Entity Java | Thuộc Phân Hệ | Nhóm Chức Năng |
|:---:|:---|:---|:---|:---|
| 1 | `roles` | `Role.java` | Core / Auth | Phân quyền vai trò (`ROLE_STUDENT`, `ROLE_STAFF`, `ROLE_ADMIN`) |
| 2 | `departments` | `Department.java` | Core / Master Data | Danh mục Phòng/Ban/Khoa tiếp nhận xử lý |
| 3 | `users` | `User.java` | Core / Auth | Quản lý người dùng, sinh viên, cán bộ, admin |
| 4 | `tickets` | `Ticket.java` | Ticket System | Quản lý yêu cầu hỗ trợ, mã ticket, SLA, trạng thái, đánh giá CSAT |
| 5 | `ticket_histories` | `TicketHistory.java` | Ticket SLA Engine | **[Bổ trợ]** Ghi vết toàn bộ lịch sử luân chuyển trạng thái Ticket |
| 6 | `posts` | `Post.java` | Feed / Forum | Bài viết chính thức từ nhà trường & Diễn đàn sinh viên |
| 7 | `post_attachments` | `PostAttachment.java` | Feed / Storage | Tệp đính kèm bài viết (PDF, Word, Excel, Video MP4) |
| 8 | `post_reports` | `PostReport.java` | Moderation | Báo cáo vi phạm nội dung diễn đàn sinh viên |
| 9 | `comments` | `Comment.java` | Feed / Forum | Bình luận đa cấp dưới bài viết |
| 10 | `likes` | `Like.java` | Feed / Forum | Lượt tương tác / thả tim bài viết |
| 11 | `conversations` | `Conversation.java` | Live Chat / AI Session | **[Bổ trợ]** Phiên hội thoại trực tiếp giữa Sinh viên - AI / Cán bộ |
| 12 | `messages` | `Message.java` | Live Chat / AI Session | **[Bổ trợ]** Danh sách tin nhắn trao đổi trong phiên chat |
| 13 | `attachments` | `Attachment.java` | Live Chat / Message | **[Bổ trợ]** Tệp tin/ảnh gửi kèm trong lúc nhắn tin chat |
| 14 | `knowledge_documents` | `KnowledgeDocument.java` | AI RAG Hub | Quản lý tài liệu nguồn tri thức (PDF quy chế, đề án tuyển sinh) |
| 15 | `knowledge_chunks` | `KnowledgeChunk.java` | AI RAG Vector | **[Bổ trợ]** Các đoạn trích đoạn (Chunks) đã phân tách & index vector |
| 16 | `faqs` | `Faq.java` | FAQ Engine | **[Bổ trợ]** Kho câu hỏi thường gặp phục vụ tra cứu nhanh |
| 17 | `internal_staff_admin_messages` | `InternalStaffAdminMessage.java` | Admin Portal | **[Bổ trợ]** Kênh chỉ đạo nghiệp vụ nội bộ giữa Admin và Staff |

---

## 2. PHÂN TÍCH CHI TIẾT 5 BẢNG "DƯ / BỔ TRỢ" - CÓ NÊN GIỮ HAY XÓA?

Nhiều đồ án chỉ dừng ở mức 12 bảng cơ bản (User, Role, Dept, Ticket, Post, Comment, Like, Report, Attachment, Doc). 5 bảng mở rộng sau đây xuất hiện trong `doancuoiki_demo1`:

### 🔹 Bảng 1: `internal_staff_admin_messages` (Trao đổi nội bộ Admin - Cán bộ)
- **Mục đích**: Cho phép Admin gửi các thông báo nội bộ, chỉ đạo nghiệp vụ, giao ban hoặc phản hồi trực tiếp cho Staff của từng Phòng/Ban mà **Sinh viên và Guest không nhìn thấy**.
- **Code đã có**: Đã có Controller `InternalMessageRestController`, Service `InternalMessageService`, Repository và UI Admin.
- **Đánh giá**: **RẤT NÊN GIỮ**. Giúp hội đồng chấm thi đánh giá cao tính năng quản trị vận hành thực tế của một cổng hỗ trợ nhà trường.

### 🔹 Bảng 2: `knowledge_chunks` (Lưu chi tiết các đoạn tri thức RAG)
- **Mục đích**: Khi nạp 1 file PDF 50 trang vào `knowledge_documents`, hệ thống cắt nhỏ thành hàng trăm đoạn `chunks` có gắn vector embedding để AI tìm kiếm ngữ nghĩa chính xác.
- **Code đã có**: Có Visualizer xem trực quan các đoạn chunks trên giao diện Admin (`/admin/knowledge/visualizer`).
- **Đánh giá**: **RẤT NÊN GIỮ**. Đây là bằng chứng rõ ràng cho thấy hệ thống thực sự tích hợp AI RAG (Retrieval-Augmented Generation) chuẩn chỉnh chứ không chỉ gọi API chatbot OpenAI/Gemini thông thường.

### 🔹 Bảng 3: `ticket_histories` (Lịch sử chuyển trạng thái SLA)
- **Mục đích**: Ghi nhận Audit Log: lúc mấy giờ, ai đã chuyển ticket từ `OPEN` sang `IN_PROGRESS`, gia hạn SLA, hoặc đóng ticket.
- **Code đã có**: Tích hợp trực tiếp vào `TicketService` khi xử lý luân chuyển vé.
- **Đánh giá**: **BẮT BUỘC NÊN GIỮ**. Môn Lập trình Web & Hệ thống thông tin rất chú trọng tính toàn vẹn dữ liệu và kiểm toán (Audit Trail) của Ticket SLA.

### 🔹 Bảng 4: `faqs` (Ngân hàng câu hỏi thường gặp)
- **Mục đích**: Cung cấp trang tra cứu FAQ nhanh cho sinh viên (`/faq/search`) không cần tốn token gọi AI, và hỗ trợ tính năng Smart FAQ Matching tự động gợi ý câu trả lời trước khi sinh viên gửi ticket.
- **Code đã có**: Controller, Repository và giao diện `search.html`.
- **Đánh giá**: **NÊN GIỮ**. Tăng trải nghiệm người dùng (UX) và giảm tải cho hệ thống xử lý ticket.

### 🔹 Bảng 5: `conversations` & `messages` (Phiên hội thoại Chat Trực tiếp / Chatbot)
- **Mục đích**: Tách biệt luồng Chat Realtime (nhiều tin nhắn liên tục) với luồng Ticket Form (vé hỗ trợ chính thức có SLA).
- **Code đã có**: Module `chat` và widget `chat-widget.html`.
- **Đánh giá**: **NÊN GIỮ**. Phân định rõ ràng giữa "Hỏi đáp nhanh qua Chat" và "Tạo phiếu hỗ trợ chính thức".

---

## 3. KẾT LUẬN & ĐỀ XUẤT HƯỚNG XỬ LÝ

| Lựa chọn | Ưu điểm | Nhược điểm | Đề xuất |
|:---|:---|:---|:---:|
| **Phương án A (Khuyên dùng): GIỮ NGUYÊN 17 BẢNG** | - Toàn bộ chức năng đã code xong và đang chạy ổn định.<br>- Tính năng phong phú (RAG Visualizer, Audit Log SLA, FAQ, Chat nội bộ).<br>- Dễ đạt điểm tối đa (9.5 - 10.0) khi báo cáo đồ án. | Schema DB lớn hơn 1 chút (nhưng đã chuẩn hóa 3NF). | ⭐⭐⭐⭐⭐ **(Được khuyến nghị)** |
| **Phương án B: LƯỢC BỎ 5 BẢNG BỔ TRỢ** | - DB gọn hơn còn 12 bảng cơ bản. | - Phải sửa lại nhiều file code Java, xoá các Service/Controller liên quan.<br>- Mất tính năng RAG Chunks Inspector và SLA Audit Trail. | ⚠️ **Không khuyến khích** (dễ phát sinh lỗi compile) |

> 📌 **Lời khuyên**: Vì `doancuoiki_demo1` **đã chạy rất mượt và hoàn chỉnh cả 17 bảng**, bạn nên **giữ nguyên toàn bộ 17 bảng**. Trong báo cáo đồ án, hãy trình bày 12 bảng là *Nhóm Chức năng Cốt lõi (Core Modules)* và 5 bảng còn lại là *Nhóm Mở rộng & Tối ưu Trải nghiệm (Advanced & SLA/RAG Engine)* để gây ấn tượng mạnh với giảng viên.

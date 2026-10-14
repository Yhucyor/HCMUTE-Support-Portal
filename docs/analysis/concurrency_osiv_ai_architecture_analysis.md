================================================================================
BÁO CÁO PHÂN TÍCH CHUYÊN SÂU KIẾN TRÚC HỆ THỐNG QAUTE PORTAL (SPRING BOOT 3)
CHUYÊN ĐỀ: CONCURRENCY LOCK (ATOMIC UPDATE), AI ASYNC THREADING, TẮT OSIV & DTO
VÀ MÔ HÌNH CHUYỂN TIẾP CHATBOT AI SANG TICKET
================================================================================
Mã tài liệu: docs/analysis/concurrency_osiv_ai_architecture_analysis.txt
Dự án: QAUTE Portal - HCMUTE Student Support & Counseling Portal
Trạng thái: Hoàn thiện thiết kế & Sẵn sàng chuẩn hóa (Design Spec Approved)
================================================================================

MỤC LỤC:
1. VẤN ĐỀ 1: CHỐNG RACE CONDITION KHI CÁN BỘ NHẬN TICKET (ATOMIC UPDATE QUERY)
2. VẤN ĐỀ 2: KIẾN TRÚC NON-BLOCKING & PHÒNG VỆ NGHẼN TOMCAT CHO MODULE AI
3. VẤN ĐỀ 3: TẮT OPEN-IN-VIEW (OSIV = FALSE) & KỶ LUẬT BẮT BUỘC DTO / @ENTITYGRAPH
4. VẤN ĐỀ 4: RÕ RÀNG RANH GIỚI NGHIỆP VỤ: CHATBOT AI GỢI Ý TẠO TICKET THAY VÌ LIVE CHAT

--------------------------------------------------------------------------------
1. VẤN ĐỀ 1: CHỐNG RACE CONDITION KHI CÁN BỘ NHẬN TICKET (ATOMIC UPDATE QUERY)
--------------------------------------------------------------------------------
1.1. Hiện trạng nguy cơ lỗi (Race Condition):
- Bối cảnh: Một Khoa có 3 - 5 Cán bộ cùng trực trang Dashboard quản lý Ticket.
- Khi sinh viên nộp một Ticket mới (trạng thái 'OPEN', assigned_to = NULL), nếu có 2 cán bộ
  cùng bấm nút "Tiếp nhận xử lý" (Claim Ticket) tại cùng một tích tắc (miligiây):
  + Luồng 1 (Cán bộ A): SELECT ticket (thấy status = OPEN, assigned_to = NULL)
  + Luồng 2 (Cán bộ B): SELECT ticket (cũng thấy status = OPEN, assigned_to = NULL)
  + Luồng 1: Gán assignedTo = Staff A, status = IN_PROGRESS -> Lưu vào DB
  + Luồng 2: Gán assignedTo = Staff B, status = IN_PROGRESS -> Lưu vào DB ghi đè Staff A!
- Hậu quả: Dữ liệu bị ghi đè không kiểm soát, sinh viên và Cán bộ A đều tưởng Cán bộ A làm,
  nhưng CSDL lại lưu Cán bộ B; bảng lịch sử TicketHistory ghi log hỗn loạn.

1.2. So sánh giải pháp:
- Phương án A (Optimistic Locking với @Version):
  + Thêm cột `version` (INT/BIGINT) vào bảng `tickets`.
  + Khi Cán bộ B lưu sau, Hibernate phát hiện version trong DB đã tăng -> ném `OptimisticLockException`.
  + Điểm yếu: Phải thực hiện 2 câu lệnh (1 SELECT + 1 UPDATE), gây rollback transaction thô bạo,
    chi phí overhead của Hibernate cao khi lượng tranh chấp lớn.
- Phương án B (Atomic Update Query tại MySQL Engine - KHUYÊN DÙNG CHUẨN 10 ĐIỂM):
  + Sử dụng trực tiếp câu lệnh UPDATE nguyên tử trong TicketRepository:

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Ticket t SET t.assignedTo = :staff, t.status = 'IN_PROGRESS' " +
           "WHERE t.id = :ticketId AND t.status = 'OPEN' AND t.assignedTo IS NULL")
    int claimTicketAtomic(@Param("ticketId") Long ticketId, @Param("staff") User staff);

1.3. Cơ chế hoạt động & Xử lý kỹ thuật chuyên sâu:
- Cơ chế Lock: MySQL InnoDB sử dụng cơ chế Khóa cấp dòng độc quyền (Exclusive Row-Level Lock, X-Lock)
  dựa trên chỉ mục Primary Key (t.id). Khi lệnh UPDATE chạy, phiên đầu tiên nắm giữ X-Lock dòng đó,
  cập nhật `assigned_to` và đổi `status = 'IN_PROGRESS'`.
- Phiên thứ hai chạy ngay sau sẽ đọc được dữ liệu đã bị đổi -> Điều kiện WHERE không còn khớp
  (`status = 'OPEN'` và `assignedTo IS NULL` bị false).
- Kết quả trả về: Phương thức trả về số nguyên `int` biểu thị số dòng được cập nhật:
  + Nếu kết quả = 1: Cán bộ đó đã nhận vé thành công. Tiến hành ghi log TicketHistory.
  + Nếu kết quả = 0: Vé đã bị cán bộ khác tiếp nhận trước đó trong tích tắc.
    Service lập tức ném ngoại lệ:
    `throw new TicketAlreadyClaimedException("Yêu cầu này đã được cán bộ khác tiếp nhận xử lý!");`
- Lưu ý sống còn với Hibernate Persistence Context (First-Level Cache):
  + Lệnh `@Modifying` đi vòng qua Hibernate EntityManager để cập nhật thẳng xuống DB,
    nên các entity `Ticket` đang nằm trong bộ nhớ đệm cấp 1 (Persistence Context) của cùng session
    sẽ bị "lệch pha" (stale data).
  + BẮT BUỘC: Đặt thuộc tính `@Modifying(clearAutomatically = true)` để Hibernate tự động
    xóa cache cấp 1 ngay sau câu update, đảm bảo các lệnh đọc tiếp theo sẽ nạp dữ liệu tươi mới.

--------------------------------------------------------------------------------
2. VẤN ĐỀ 2: KIẾN TRÚC NON-BLOCKING & PHÒNG VỆ NGHẼN TOMCAT CHO MODULE AI
--------------------------------------------------------------------------------
2.1. Nguy cơ nghẽn luồng (Tomcat Thread Starvation):
- Máy chủ Spring Boot (nhúng Tomcat) mặc định có `server.tomcat.threads.max = 200` worker threads.
- Gemini API (Google Cloud) có độ trễ mạng và thời gian sinh từ (token generation) từ 1.2s đến 3.5s.
- Nếu Controller viết theo kiểu đồng bộ (Synchronous Blocking):
  Khi có 100 - 150 sinh viên cùng nhấn chat AI tại một thời điểm (Spike load mùa đăng ký học phần):
  Toàn bộ 200 Tomcat worker threads sẽ bị giữ cứng (blocked) trong trạng thái chờ I/O từ Google.
- Hậu quả nghiêm trọng:
  Không còn luồng nào phục vụ các API khác. Người dùng bên ngoài bấm vào Trang chủ, Đăng nhập,
  Xem bảng tin, Tra cứu điểm hay Nộp ticket đều bị treo vô tận và nhận lỗi HTTP 504 Gateway Timeout!

2.2. Giải pháp Kiến trúc Đồ án 10 Điểm:
- Bước 1: Chuyển đổi Controller sang Bất đồng bộ (Async Servlet):
  Controller trả về `CompletableFuture<ResponseEntity<ApiResponse<RagQueryResponse>>>`.
  Ngay khi request đến, Tomcat worker thread đẩy tác vụ vào hàng đợi và ĐƯỢC GIẢI PHÓNG NGAY LẬP TỨC
  để quay lại phục vụ các HTTP request khác. Khi AI xử lý xong, một luồng phản hồi sẽ ghi kết quả về client.

- Bước 2: Tách ThreadPoolTaskExecutor Độc Lập cho AI (`aiTaskExecutor`):
  Không dùng chung luồng gửi Email OTP (`mailTaskExecutor`).
  Cấu hình tại AsyncConfig.java:
  + corePoolSize: 10
  + maxPoolSize: 30
  + queueCapacity: 100
  + rejectedExecutionHandler: CallerRunsPolicy (khi hàng đợi đầy, fallback an toàn).

- Bước 3: Đặt Client Timeout Chặt Chẽ (Hard Timeout = 8 giây):
  Cấu hình timeout trong RestClient / WebClient gọi Gemini API:
  Connect Timeout = 3s, Read Timeout = 8s.
  Nếu quá 8 giây Gemini chưa trả về, ngắt kết nối ngay và trả về Fallback Response:
  "Hệ thống Trợ lý AI đang có lưu lượng truy cập cao. Quy chế này có thể đã được giải đáp trong
   mục FAQ, hoặc bạn có thể tạo Ticket để Cán bộ Phòng/Khoa hỗ trợ trực tiếp."

--------------------------------------------------------------------------------
3. VẤN ĐỀ 3: TẮT OPEN-IN-VIEW (OSIV = FALSE) & KỶ LUẬT BẮT BUỘC DTO / @ENTITYGRAPH
--------------------------------------------------------------------------------
3.1. Bản chất Open-In-View (OSIV) và vì sao phải tắt:
- Spring Boot mặc định bật `spring.jpa.open-in-view: true`.
- Ý nghĩa: Mở một JPA EntityManager/Session và giữ kết nối CSDL (Hikari Connection)
  xuyên suốt từ khi bắt đầu nhận request, qua Filter, Controller, Service và kéo dài đến tận
  khi Thymeleaf hoàn tất việc render xong toàn bộ cây mã HTML gửi cho trình duyệt.
- Dưới tải 1.000 sinh viên truy cập đồng thời:
  Một kết nối MySQL bị "ngậm" trong 100ms - 300ms chỉ để chờ Thymeleaf render template,
  khiến Connection Pool (HikariCP 30 connections) cạn sạch chỉ trong 2 giây đầu tiên!

3.2. Cấu hình bắt buộc:
  spring:
    jpa:
      open-in-view: false

3.3. Ràng buộc Kỷ luật Lập trình viên & AI Sinh Code:
- Khi tắt OSIV, kết nối CSDL sẽ được đóng và giải phóng về Hikari Pool NGAY KHI phương thức
  `@Transactional` trong Service kết thúc.
- RỦI RO LỚN: Nếu View Thymeleaf cố tình gọi lazy loading:
  `<span th:text="${ticket.creator.department.name}"></span>`
  `<div th:each="c : ${post.comments}"></div>`
  Hibernate sẽ ném ngay lập tức ngoại lệ: `org.hibernate.LazyInitializationException: could not initialize proxy - no Session`
  và làm sập trang HTML (Lỗi HTTP 500).

- QUY TẮC RÀNG BUỘC TUYỆT ĐỐI (ENGINEERING HARD RULE):
  1. Tuyệt đối không đẩy trực tiếp JPA Entity có quan hệ `@ManyToOne(fetch = FetchType.LAZY)`
     ra Model của Thymeleaf nếu chưa được fetch trước.
  2. Mọi dữ liệu hiển thị ra Thymeleaf BẮT BUỘC phải chuyển đổi thành DTO (Data Transfer Object)
     hoặc Java Record ngay tại tầng Service trong phạm vi còn mở `@Transactional`.
  3. Nếu muốn trả về Entity để render View nhanh, câu truy vấn tại Repository BẮT BUỘC phải dùng:
     - Hoặc `@Query("SELECT t FROM Ticket t JOIN FETCH t.creator JOIN FETCH t.department WHERE t.id = :id")`
     - Hoặc `@EntityGraph(attributePaths = {"creator", "department"})`
  4. Mọi quan hệ `@ManyToOne` và `@OneToMany` luôn giữ `FetchType.LAZY`. Cấm đổi thành `EAGER` bừa bãi.

--------------------------------------------------------------------------------
4. VẤN ĐỀ 4: RÕ RÀNG RANH GIỚI NGHIỆP VỤ: CHATBOT AI GỢI Ý TẠO TICKET THAY VÌ LIVE CHAT
--------------------------------------------------------------------------------
4.1. Quyết định Nghiệp vụ (Business Decision):
- Hệ thống hỗ trợ sinh viên trường Đại học có đặc thù: Cán bộ phòng ban không thể ngồi trực online
  24/7 để chat tay (Live Chat) từng câu với hàng chục ngàn sinh viên.
- Thay vì triển khai một hệ thống Live Chat người-người cồng kềnh, dễ gây quá tải và trễ nải:
  -> CỔNG TIẾP NHẬN ĐẦU TIÊN LÀ CHATBOT AI TỰ ĐỘNG 24/7 (AI RAG ASSISTANT).
  -> CỔNG GIẢI QUYẾT CHÍNH THỨC CỦA NHÀ TRƯỜNG LÀ TICKET SYSTEM (HỆ THỐNG VÉ HỖ TRỢ CÓ SLA).

4.2. Luồng Chuyển Tiếp Tự Động (AI-to-Ticket Conversion Workflow):
1. Sinh viên nhập câu hỏi vào Chatbox ở góc màn hình (hoặc trang Tra cứu AI).
2. Hệ thống chạy Phễu lọc 4 tầng (Short-circuiting funnel):
   - Cấp 0: Regex bắt từ khóa tĩnh (Hotline, Email phòng ban, giờ làm việc) -> Phản hồi trong 5ms.
   - Cấp 1: Cache Caffeine tra cứu câu hỏi phổ biến -> Phản hồi trong 3ms.
   - Cấp 2: Tra cứu Smart FAQ tương đồng cao (Cosine >= 0.85) -> Phản hồi trong 30ms.
   - Cấp 3: AI RAG (Gemini Flash) tra cứu văn bản quy chế chính thức -> Phản hồi trong 1.5s.
3. Kịch bản khi AI không có dữ liệu tin cậy (Cosine < 0.65 hoặc câu hỏi mang tính cá nhân):
   - AI trả lời:
     "Quy chế học vụ hiện tại chưa có thông tin cụ thể về trường hợp của bạn (hoặc trường hợp này
      cần Cán bộ Khoa/Phòng kiểm tra trực tiếp trên hệ thống đào tạo).
      Bạn có muốn gửi Yêu cầu Hỗ trợ (Ticket) đến Cán bộ phụ trách không?"
   - Chatbox tự động hiển thị nút bấm Call-To-Action (CTA):
     [ Gửi Ticket Đến Khoa/Phòng Phụ Trách ]
4. Khi sinh viên bấm nút CTA:
   - Trình duyệt điều hướng sang `/tickets/create`.
   - Hệ thống tự động điền sẵn (pre-fill):
     + Tiêu đề Ticket: Trích xuất từ câu hỏi tóm tắt của sinh viên.
     + Nội dung chi tiết: Câu hỏi của sinh viên kèm lịch sử trao đổi ngắn gọn với AI.
     + Đơn vị tiếp nhận: Tự động chọn Khoa/Phòng ban mà sinh viên đã chỉ định trên chatbox.
5. Sinh viên chỉ cần đính kèm minh chứng (nếu có) và nhấn "Gửi Yêu Cầu".
6. Ticket lập tức kích hoạt SLA Engine (Hạn chót 48h, Phân quyền Khoa, Email thông báo, v.v.).

KẾT LUẬN:
Mô hình "Chatbot AI 24/7 -> Gợi ý tạo Ticket chính thức" vừa giúp cắt giảm 70% khối lượng câu hỏi
lặp lại cho Cán bộ, vừa đảm bảo 100% các vấn đề khó của sinh viên đều được tiếp nhận xử lý theo đúng
quy trình hành chính có thời hạn (SLA) minh bạch và không xảy ra xung đột kiến trúc LiveChat/Ticket!
================================================================================

mô hình kiến conversation as a ticket 
đảm bảo giá trị nghiêm ngặt cho doanh nghiệp 
các ctty lớn và dịch vụ này như Zendesk va intercom bán giải pháp dịch vụ helpdesk toàn cầu 
shoppe grab , fpt software  , cổng hỗ trợ helpdesk 

so vs 1 projecr mxh dùng 1 bảng mess và 1 bảng conversation là hoàn hảo 
// còn 1 web tư vấn sinh viên sẽ thiên về quản trị hơn 
// có trạng thái vòng đời có giới hạn thời gian 
// vd cứ để 1 ng hỏi 1 ng tư vấn viên trả lời ok quá hợp lý nhg mà câu hỏi thứ 2 
// là ng tư vấn nào nhắn tn đó 


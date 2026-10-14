# ================================================================================
# BÁO CÁO PHÂN TÍCH KỸ THUẬT: HIỆN TRẠNG INGESTION 91 CÔNG VĂN & CÁC RỦI RO TIỀM ẨN
# ================================================================================
# Mã tài liệu: docs/analysis/rag_91_documents_ingestion_and_risk_analysis.md
# Dự án: QAUTE Portal - HCMUTE Student Support & Counseling Portal
# Ngày ghi nhận: 02/10/2026
# Trạng thái: Điều tra hoàn tất & Xác định phương án khắc phục (RCA Completed)
# ================================================================================

## MỤC LỤC
1. Hiện trạng thực tế kho dữ liệu công văn (Database & ChromaDB)
2. Phân tích nguyên nhân gốc rễ (Root Cause Analysis - RCA)
3. Danh mục 5 rủi ro tiềm ẩn đe dọa hệ thống
4. Phương án khắc phục theo 4 giai đoạn kiểm thử nghiêm ngặt

---

## 1. HIỆN TRẠNG THỰC TẾ KHO DỮ LIỆU CÔNG VĂN (02/10/2026)

Đối soát giữa kho file vật lý tại `D:\HK5\CongNghePhanMem\tailieuAI` và Cơ sở dữ liệu:

### 1.1. Thư mục file PDF gốc (`D:\HK5\CongNghePhanMem\tailieuAI`)
- Năm 2024: 14 tệp PDF (Quy chế học phí, chuyển đổi điểm ngoại ngữ, lịch thi GDQP...).
- Năm 2025: 14 tệp PDF (Thông báo tuyển sinh, giảm học phí...).
- Năm 2026: 63 tệp PDF (Kế hoạch sinh hoạt đầu khóa, đồ án tốt nghiệp, chuẩn đầu ra...).
- **Tổng cộng:** **91 tập tin PDF công văn chính thức**.

### 1.2. Cơ sở dữ liệu MySQL (`qaute_portal`)
Truy vấn trực tiếp trên hai bảng `knowledge_documents` và `knowledge_chunks`:
- **Bảng `knowledge_documents`:**
  - Tổng số bản ghi: **68 / 91** văn bản.
  - Số văn bản đã nạp thành công (`status = 'COMPLETED'`): **3 văn bản** (ID 1, 2, 3 - tổng 31 chunks).
  - Số văn bản đang bị treo (`status = 'PROCESSING'`): **65 văn bản** (tất cả đều có `total_chunks = 0`).
  - Số văn bản chưa từng được quét tới: **23 văn bản**.
- **Bảng `knowledge_chunks`:**
  - Tổng số chunk: **331 chunks**, gồm:
    - 300 chunks từ bộ câu hỏi FAQ hạt nhân (`source_type = 'FAQ_CHAT'`).
    - 31 chunks từ 3 công văn đầu tiên (`source_type = 'REGULATION'`).
- **Kho ChromaDB (`python-ai-engine/data/chroma_db`):**
  - Thư mục `markdown_docs` rỗng (0 file `.md`).
  - `chroma.sqlite3` chỉ chứa đúng 1 embedding test.

👉 **Kết luận:** **91 tài liệu công văn CHƯA ĐƯỢC CHUNK ĐẦY ĐỦ.** Quy trình nạp bị lỗi và dừng lại giữa chừng từ văn bản số 4 trở đi.

---

## 2. PHÂN TÍCH NGUYÊN NHÂN GỐC RỄ (ROOT CAUSE ANALYSIS - RCA)

### RCA-01: Lỗi Tràn Chiều Dài Cột Cơ Sở Dữ Liệu (`DataTruncationException`)
- Trong cấu hình JPA:
  - Cột `knowledge_chunks.title`: `VARCHAR(255)`
  - Cột `knowledge_chunks.article_header`: `VARCHAR(255)`
  - Cột `knowledge_documents.document_code`: `VARCHAR(100)`
- Trong logic cũ của `BatchDocumentIngestionService`:
  - `chunkTitle = cleanTitle + " - Trang " + sc.pageNumber() + " (#" + chunkSeq++ + ")"`: Với các tệp có tên dài kèm số trang, chuỗi sinh ra dễ dàng vượt quá 255 ký tự.
  - `docCode = PdfExtractorUtils.extractDocumentCode(firstPageText)`: Regex trích xuất đôi khi bắt trúng cả dòng tiêu đề dài dẫn đến chuỗi > 100 ký tự.
- Khi Spring Data JPA thực thi `chunkRepository.saveAll(chunksToSave)`, Hibernate ném ngoại lệ `DataIntegrityViolationException: Data truncation`.

### RCA-02: Lỗi Quản Lý Transaction Khiến Document Treo Ở Trạng Thái `PROCESSING`
- Trong hàm `doIngestSingleDocument`:
  - Entity `KnowledgeDocument` được cập nhật `status = 'PROCESSING'` và gọi `documentRepository.save(document)`.
  - Tiếp theo là khối băm chunk và lưu `chunkRepository.saveAll(...)`.
  - Trước đây, khối này không có `try-catch` riêng biệt; ngoại lệ ném ra ngoài và bị bắt tại `transactionTemplate.execute(...)`.
  - Do transaction không đánh dấu rollback tường minh (`status.setRollbackOnly()`), bản ghi `KnowledgeDocument` với trạng thái `PROCESSING` vẫn được commit xuống database.
- Hậu quả: 65 tài liệu lưu dở dang trong DB, không có chunk nào, và khi quét lại hệ thống thấy bản ghi đã tồn tại nhưng không kiểm tra kỹ trạng thái nên bị bỏ qua hoặc lỗi tiếp.

### RCA-03: Điều Kiện Khởi Động Sai Trong `DataInitializer.java` Chặn Nạp Lại
- Tại `DataInitializer.java` dòng 136:
  ```java
  long currentDocCount = knowledgeDocumentRepository.count();
  if (currentDocCount < 50) {
      CompletableFuture.runAsync(() -> batchDocumentIngestionService.ingestAllYearFolders("D:\\HK5\\CongNghePhanMem\\tailieuAI"));
  }
  ```
- Do trong DB đã có sẵn 68 bản ghi (dù chỉ có 3 bản ghi thành công và 65 bản ghi dở dang), `currentDocCount = 68 >= 50`.
- Do đó, mỗi khi ứng dụng khởi động lại, đoạn mã tự động nạp **bị bỏ qua vĩnh viễn**, khiến 65 tài liệu mãi mãi kẹt ở `PROCESSING`.

---

## 3. DANH MỤC 5 RỦI RO TIỀM ẨN ĐE DỌA HỆ THỐNG

| Mã Rủi Ro | Phân Loại | Mức Độ | Nguy Cơ Tiềm Ẩn & Tác Động |
| :---: | :--- | :---: | :--- |
| **RISK-01** | Database & Integrity | **CRITICAL** | **Dữ liệu treo gây mù thông tin:** 65/91 công văn kẹt ở `PROCESSING` khiến Chatbot không có dữ liệu để trả lời các quy định học vụ năm 2024-2025. |
| **RISK-02** | External API / Quota | **HIGH** | **Tràn Rate Limit Gemini API (HTTP 429):** Nạp đồng loạt 91 văn bản (~1.500 chunks) với Free Tier (15 RPM) sẽ khiến API trả về 429. Hệ thống tự rơi về vector băm toán học giả lập (`generateDeterministicVector`), làm triệt tiêu khả năng tìm kiếm ngữ nghĩa chính xác. |
| **RISK-03** | Memory / Performance | **HIGH** | **Áp lực RAM In-Memory Vector Cache:** Tải toàn bộ 1.500 vector 768 chiều float lên RAM kết hợp JVM và IDE làm tăng mức sử dụng RAM > 85%, dễ chạm ngưỡng OOM nếu không cấu hình giới hạn `-Xmx`. |
| **RISK-04** | Architecture Divergence | **MEDIUM** | **Lệch pha Dual-Engine (Java vs Python):** Cờ `app.python-ai.enabled` nếu bật lên sẽ làm hệ thống lỗi vì ChromaDB chưa có dữ liệu công văn. |
| **RISK-05** | Hibernate / OSIV | **MEDIUM** | **Vi phạm OSIV = false:** Truy xuất Lazy Relations ngoài Transaction khi hiển thị danh sách Document hoặc Chunks trên giao diện Admin dễ gây `LazyInitializationException`. |

---

## 4. PHƯƠNG ÁN KHẮC PHỤC THEO 4 GIAI ĐOẠN KIỂM THỬ NGHIÊM NGẶT

### Giai đoạn 1: Chuẩn Hóa Mã Nguồn & Phòng Vệ Cắt Chuỗi
- Đảm bảo 100% các chuỗi `title`, `documentCode`, `articleHeader` được cắt ngắn an toàn trước khi insert.
- Bọc `try-catch` cục bộ từng văn bản, nếu lỗi chunking phải gán ngay `status = 'FAILED'` kèm `errorMessage` chi tiết.
- Cập nhật điều kiện kích hoạt trong `DataInitializer`: Đếm số tài liệu `COMPLETED` thay vì đếm tổng số bản ghi:
  `knowledgeDocumentRepository.countByStatus("COMPLETED") < 80`.
- **Kiểm thử Giai đoạn 1:** Chạy `mvn test-compile` và chạy bộ kiểm thử unit `BatchDocumentIngestionServiceTest`.

### Giai đoạn 2: Reset & Làm Sạch Dữ Liệu Treo Trên Database
- Xóa các bản ghi `PROCESSING` và các chunk rác mồ côi (nếu có) bằng hàm chuẩn hoặc query dọn dẹp.
- Giữ nguyên 300 FAQ hạt nhân chuẩn.
- **Kiểm thử Giai đoạn 2:** Query kiểm tra MySQL đảm bảo bảng `knowledge_documents` chỉ còn trạng thái hợp lệ hoặc sạch sẽ.

### Giai đoạn 3: Thực Thi Ingestion An Toàn & Điều Tiết Tốc Độ (Rate Limiting)
- Chèn cơ chế điều tiết nhịp gọi (Rate-limiting delay 100 - 150ms giữa các chunk) hoặc xử lý theo lô an toàn.
- Kích hoạt batch ingestion quét trọn vẹn 91 file tại `D:\HK5\CongNghePhanMem\tailieuAI`.
- **Kiểm thử Giai đoạn 3:** Đối soát MySQL xác nhận:
  - `knowledge_documents`: $\ge 85$ văn bản chuyển sang `COMPLETED`.
  - `knowledge_chunks`: Tăng lên $> 800 - 1.200$ chunks `REGULATION`.

### Giai đoạn 4: Kiểm Thử Tích Hợp RAG Search, Citation & Bộ Nhớ RAM
- Kiểm tra tính năng tìm kiếm Cosine Similarity trên RAM.
- Đảm bảo trích dẫn nguồn văn bản (Citation) trả lời đúng câu hỏi học vụ thực tế.
- Đo đạc lại mức tiêu thụ RAM, đảm bảo heap ổn định.

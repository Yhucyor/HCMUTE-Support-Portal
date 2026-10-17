# TÀI LIỆU DỮ LIỆU ĐÃ CHẮT LỌC VÀ CHUYỂN ĐỔI GIAO DIỆN (HCMUTE / QAUTE PORTAL)

---

## 1. BẢNG DANH SÁCH 18 ĐƠN VỊ & PHÒNG BAN CHUẨN (SEED DATA `departments`)

Dữ liệu đã trích xuất sạch sẽ từ hệ thống cũ, đánh mã code chuẩn cho Spring Boot:

| ID | Mã Code | Tên Đơn vị / Khoa / Phòng ban | Loại đơn vị |
| :--- | :--- | :--- | :--- |
| **1** | `FAC_IT` | Khoa Công Nghệ Thông Tin | Khoa Chuyên môn |
| **2** | `FAC_FME` | Khoa Cơ Khí Chế Tạo Máy | Khoa Chuyên môn |
| **3** | `FAC_FL` | Khoa Ngoại Ngữ | Khoa Chuyên môn |
| **4** | `FAC_FGF` | Khoa Công Nghệ May & Thời Trang | Khoa Chuyên môn |
| **5** | `FAC_FEEE` | Khoa Điện - Điện Tử | Khoa Chuyên môn |
| **6** | `FAC_FVE` | Khoa Cơ Khí Động Lực | Khoa Chuyên môn |
| **7** | `DEPT_ACADEMIC` | Phòng Đào Tạo | Phòng Ban Học Vụ |
| **8** | `FAC_FCE` | Khoa Xây Dựng | Khoa Chuyên môn |
| **9** | `FAC_FE` | Khoa Kinh Tế | Khoa Chuyên môn |
| **10** | `FAC_PRINT` | Khoa In - Truyền Thông | Khoa Chuyên môn |
| **11** | `FAC_CHEM` | Khoa Công Nghệ Hóa - Thực Phẩm | Khoa Chuyên môn |
| **12** | `FAC_FHQ` | Khoa Đào Tạo Chất Lượng Cao | Khoa Chuyên môn |
| **15** | `CTR_STU_SERVICE`| Trung Tâm Dịch Vụ Sinh Viên - Tư Vấn Học Đường | Trung Tâm Hỗ Trợ |
| **16** | `DEPT_ADMISSION` | Phòng Tuyển Sinh và Công Tác Sinh Viên - Chế Độ Chính Sách | Tuyển Sinh & CTSV |
| **17** | `ORG_YOUTH_UNION`| Đoàn Thanh Niên - Hội Sinh Viên | Đoàn Thể Phong Trào |
| **18** | `FAC_FAS` | Khoa Khoa Học Ứng Dụng | Khoa Chuyên môn |
| **19** | `INST_TE` | Viện Sư Phạm Kỹ Thuật | Viện Đào Tạo |
| **20** | `FAC_FIE` | Khoa Đào Tạo Quốc Tế | Khoa Chuyên môn |

---

## 2. DANH SÁCH CÁN BỘ / TƯ VẤN VIÊN THỰC TẾ (SEED DATA `users`)

| Username | Họ và tên | Chức vụ / Phòng Ban | Email | Ghi chú avatar |
| :--- | :--- | :--- | :--- | :--- |
| `binhlq` | **Thầy Lê Quang Bình** | Cán bộ Phòng Đào tạo | `binhlq@hcmute.edu.vn` | Ảnh đại diện thật cán bộ P. Đào tạo |
| `vangdq` | **Thầy Đàng Quang Vắng** | Cán bộ Khoa Kinh tế (Văn phòng A1-306) | `vangdq@hcmute.edu.vn` | Tư vấn học bổng, chương trình ngành KT |
| `hapt` | **Thầy Hà** | Cán bộ Kỹ thuật & Quản trị HT Online | `haspkt@hcmute.edu.vn` | Hỗ trợ tài khoản sinh viên online |
| `nhipham` | **Chị Nhi** | Cán bộ Hỗ trợ Hồ sơ & Tuyển sinh | `nhipham@hcmute.edu.vn` | Tiếp nhận phiếu điểm & thủ tục xét tuyển |

---

## 3. BỘ DỮ LIỆU CÂU HỎI HỌC VỤ CHUẨN (ĐÃ LÀM SẠCH HTML & LỌC TRÙNG LẶP)
*Dùng làm Seed Data cho Ticket/Q&A và làm tập tri thức Vector Database cho AI RAG:*

### 🔹 Câu hỏi 1: Chế độ hỗ trợ chi phí học tập & Dân tộc thiểu số
- **Sinh viên:** Danh Anh Thư
- **Đơn vị tiếp nhận:** Phòng Tuyển sinh và Công Tác Sinh Viên (Chế độ chính sách)
- **Nội dung hỏi:** *"Em đã được hưởng chế độ miễn giảm học phí thuộc đối tượng dân tộc thiểu số và có hộ cận nghèo, vậy em có được hưởng thêm chế độ trợ cấp chi phí học tập không ạ?"*
- **Cán bộ phản hồi (Thầy Lê Quang Bình):**  
  *"Chào em! Căn cứ Quyết định số 66/2013/QĐ-TTg của Thủ tướng Chính phủ: Sinh viên là người dân tộc thiểu số thuộc hộ nghèo hoặc cận nghèo theo quy định được hưởng chính sách hỗ trợ chi phí học tập (trừ hệ đào tạo liên thông, văn bằng 2). Nếu em không thuộc các đối tượng bị trừ ra thì em hoàn toàn được hưởng hỗ trợ này."*

### 🔹 Câu hỏi 2: Nộp hồ sơ xét học bổng anh/chị em ruột & Địa chỉ Khoa Kinh tế
- **Sinh viên:** Trần Khải Hoàn (K21 - Ngành Kinh doanh quốc tế)
- **Đơn vị tiếp nhận:** Khoa Kinh Tế
- **Nội dung hỏi:** *"Em xin hỏi cách thức viết đơn xin xét học bổng anh/chị em ruột và văn phòng Khoa Kinh tế hiện ở vị trí nào ạ?"*
- **Cán bộ phản hồi (Thầy Đàng Quang Vắng):**  
  *"Chào em! Văn phòng Khoa Kinh tế nằm tại Tầng 3 - Phòng A1-306 (Tòa nhà Trung tâm). Em trực tiếp ghé văn phòng Khoa trong giờ hành chính để được thầy cô phát mẫu đơn và hướng dẫn chi tiết nhé."*

### 🔹 Câu hỏi 3: Hướng dẫn nhận tiền trợ cấp khó khăn qua thẻ BIDV
- **Sinh viên:** Nguyễn Phan Kiều Diễm (Khoa Kinh tế - MSSV: 21126120)
- **Đơn vị tiếp nhận:** Phòng Tuyển sinh và Công Tác Sinh Viên
- **Nội dung hỏi:** *"Trường đã chuyển tiền trợ cấp khó khăn HK1 vào tài khoản BIDV nhưng em đang ở quê chưa lên trường nhận thẻ trực tiếp được. Em phải làm sao để nhận tiền ạ?"*
- **Cán bộ phản hồi (Thầy Đàng Quang Vắng - Đã loại bỏ 3 bản ghi trùng lặp):**  
  *"Chào em! Em chỉ cần mang CCCD/CMND bản gốc ra chi nhánh ngân hàng BIDV gần nhất tại địa phương để làm thủ tục xác minh tài khoản và rút tiền mặt nhé."*

### 🔹 Câu hỏi 4: Phân biệt ngành Kinh doanh quốc tế và Tài chính ngân hàng
- **Sinh viên / Thí sinh:** Phạm Mai Thanh Thương
- **Đơn vị tiếp nhận:** Khoa Kinh Tế / Phòng Tuyển sinh
- **Nội dung hỏi:** *"Học Tài chính có làm việc ở doanh nghiệp ngoài ngân hàng được không? Học Kinh doanh quốc tế có làm việc tại ngân hàng được không?"*
- **Cán bộ phản hồi (Thầy Đàng Quang Vắng):**  
  *"Chào em! Ngành Tài chính - Kế toán có thể làm việc tại công ty chứng khoán, ngân hàng, bảo hiểm hoặc các phòng Kế toán - Tài chính của mọi doanh nghiệp. Sinh viên tốt nghiệp ngành Kinh doanh quốc tế hoàn toàn có thể làm việc tại Ngân hàng ở bộ phận Tài trợ thương mại, Xuất nhập khẩu hoặc Kinh doanh ngoại hối."*

### 🔹 Câu hỏi 5: Quy đổi điểm chứng chỉ TOEIC cho môn Tiếng Anh thương mại
- **Sinh viên:** Tôn Anh Huy (Khoa Kinh tế - MSSV: 20136084)
- **Đơn vị tiếp nhận:** Khoa Kinh Tế
- **Nội dung hỏi:** *"Môn Anh văn Thương mại bên Khoa Kinh tế có được dùng chứng chỉ TOEIC quốc tế để nộp quy đổi điểm môn học không ạ?"*
- **Cán bộ phản hồi (Thầy Đàng Quang Vắng):**  
  *"Chào em! Đối với Khoa Kinh tế, chứng chỉ TOEIC chỉ phục vụ xét chuẩn đầu ra tốt nghiệp, không áp dụng quy đổi thay thế cho điểm môn Tiếng Anh thương mại trong chương trình đào tạo."*

### 🔹 Câu hỏi 6: Thủ tục tân sinh viên nhập học và tải đề cương môn học
- **Sinh viên:** Từ Công Tính / Từ Thị Thúy Quyên (Khóa 2020)
- **Đơn vị tiếp nhận:** Phòng Đào Tạo
- **Nội dung hỏi:** *"Tân sinh viên đã làm thủ tục nhưng chưa có giấy báo bản cứng, không đăng nhập được để xem thời khóa biểu và tải đề cương môn học?"*
- **Cán bộ phản hồi (Thầy Lê Quang Bình):**  
  *"Chào em! Em không cần chờ giấy báo bản cứng. Em tra cứu MSSV tại cổng tracuuxettuyen.hcmute.edu.vn, làm thủ tục online tại nhaphoc.hcmute.edu.vn. Xem thời khóa biểu tại sao.hcmute.edu.vn và tải đề cương tại fe.hcmute.edu.vn. Nếu gặp lỗi tài khoản online, liên hệ Thầy Hà: haspkt@hcmute.edu.vn - 0913.889.739."*

---

## 4. BẢN THIẾT KẾ GIAO DIỆN BOOTSTRAP 5 TỐI GIẢN (KẾ THỪA BỐ CỤC 3 CỘT)

### Đặc điểm chuyển đổi công nghệ:
- ❌ **Loại bỏ:** Bootstrap 3 cũ (`panel`, `panel-warning`, `pull-right`, `badge-blue`, inline CSS rác).
- ✅ **Nâng cấp Bootstrap 5:** 
  - `card` sạch sẽ, border xám nhạt `border-light-subtle`.
  - Bộ badge chuẩn (`badge bg-primary`, `badge bg-success`).
  - Accordion chuẩn Bootstrap 5 không bị xung đột JS.
  - Tích hợp khung **AI RAG Assistant Widget** và **Bảng tin Đa phương tiện** ở cột phải.

### Cấu trúc Layout 3 Cột (Thymeleaf/Bootstrap 5):
```html
<div class="container-fluid py-4">
    <div class="row g-3">
        <!-- CỘT 1 (3 cột): DANH BẠ ĐƠN VỊ / KHOA / PHÒNG BAN -->
        <div class="col-lg-3 col-md-4">
            <div class="card shadow-sm border-0">
                <div class="card-header bg-primary text-white py-3">
                    <h6 class="mb-0 fw-bold"><i class="bi bi-grid-fill me-2"></i>Đơn Vị & Khoa / Phòng</h6>
                </div>
                <div class="list-group list-group-flush small">
                    <a th:each="dep : ${departments}" 
                       th:href="@{/tickets(deptId=${dep.id})}" 
                       class="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-2"
                       th:classappend="${selectedDeptId == dep.id} ? 'active' : ''">
                        <span th:text="${dep.name}">Khoa Công Nghệ Thông Tin</span>
                        <span class="badge bg-secondary rounded-pill" th:text="${dep.ticketCount}">12</span>
                    </a>
                </div>
            </div>
        </div>

        <!-- CỘT 2 (6 cột): BẢNG TIN HỎI ĐÁP Q&A & TICKET CÔNG KHAI -->
        <div class="col-lg-6 col-md-8">
            <div class="card shadow-sm border-0 mb-3">
                <div class="card-body p-3">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="fw-bold text-dark mb-0"><i class="bi bi-chat-left-text-fill text-primary me-2"></i>Câu Hỏi & Tư Vấn Mới Nhất</h5>
                        <a th:href="@{/ticket/create}" class="btn btn-sm btn-primary"><i class="bi bi-plus-circle me-1"></i>Gửi câu hỏi mới</a>
                    </div>
                    <!-- Search bar -->
                    <div class="input-group input-group-sm mb-3">
                        <input type="text" class="form-control" placeholder="Tìm kiếm câu hỏi, chính sách, học bổng...">
                        <button class="btn btn-outline-secondary" type="button"><i class="bi bi-search"></i></button>
                    </div>

                    <!-- Accordion Q&A Items -->
                    <div class="accordion" id="faqAccordion">
                        <!-- Lặp qua danh sách Ticket / FAQ -->
                        <div class="accordion-item mb-2 border rounded" th:each="t : ${tickets}">
                            <h2 class="accordion-header">
                                <button class="accordion-button collapsed py-2 px-3" type="button" data-bs-toggle="collapse" th:data-bs-target="'#collapse' + ${t.id}">
                                    <div class="d-flex flex-column w-100 me-3">
                                        <div class="d-flex justify-content-between align-items-center mb-1">
                                            <span class="badge bg-light text-primary border" th:text="${t.department.name}">P. Đào Tạo</span>
                                            <span class="text-muted small" th:text="${#temporals.format(t.createdAt, 'dd/MM/yyyy HH:mm')}">13/02/2023</span>
                                        </div>
                                        <strong class="text-dark" th:text="${t.title}">Trợ cấp học phí sinh viên dân tộc thiểu số</strong>
                                    </div>
                                </button>
                            </h2>
                            <div th:id="'collapse' + ${t.id}" class="accordion-collapse collapse" data-bs-parent="#faqAccordion">
                                <div class="accordion-body bg-light p-3">
                                    <!-- Câu hỏi SV -->
                                    <div class="p-3 bg-white border rounded mb-2">
                                        <div class="d-flex align-items-center mb-1">
                                            <strong class="text-primary small" th:text="${t.creatorName}">Danh Anh Thư</strong>
                                            <span class="badge bg-info-subtle text-info ms-2">Sinh viên</span>
                                        </div>
                                        <p class="mb-0 text-secondary" th:text="${t.description}">Nội dung câu hỏi...</p>
                                    </div>
                                    <!-- Phản hồi Cán bộ -->
                                    <div class="p-3 bg-primary-subtle border border-primary-subtle rounded" th:if="${t.response != null}">
                                        <div class="d-flex align-items-center mb-1">
                                            <strong class="text-dark small" th:text="${t.responderName}">Thầy Lê Quang Bình</strong>
                                            <span class="badge bg-primary text-white ms-2">Cán bộ tư vấn</span>
                                        </div>
                                        <p class="mb-0 text-dark" th:text="${t.response}">Nội dung giải đáp...</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- CỘT 3 (3 cột): BẢNG TIN ĐA PHƯƠNG TIỆN & TRỢ LÝ AI RAG -->
        <div class="col-lg-3 col-md-12">
            <!-- Widget AI RAG -->
            <div class="card shadow-sm border-0 mb-3 bg-primary text-white">
                <div class="card-body p-3">
                    <h6 class="fw-bold mb-2"><i class="bi bi-robot me-2"></i>Trợ Lý AI RAG Học Vụ</h6>
                    <p class="small text-white-50 mb-3">Hỏi đáp nhanh mọi quy chế đào tạo, điểm số và thông tin tuyển sinh tự động 24/7.</p>
                    <button class="btn btn-light btn-sm w-100 fw-bold text-primary" data-bs-toggle="modal" data-bs-target="#aiChatModal">
                        <i class="bi bi-chat-dots-fill me-1"></i>Mở Chatbot AI
                    </button>
                </div>
            </div>

            <!-- Widget Thông báo & Video Đoàn trường/Tuyển sinh -->
            <div class="card shadow-sm border-0">
                <div class="card-header bg-white border-bottom py-2">
                    <h6 class="mb-0 fw-bold text-dark"><i class="bi bi-megaphone-fill text-danger me-2"></i>Thông Báo & Video Mới</h6>
                </div>
                <div class="card-body p-2">
                    <!-- Post Item -->
                    <div class="p-2 border-bottom" th:each="post : ${officialPosts}">
                        <span class="badge bg-danger-subtle text-danger small mb-1" th:text="${post.department.name}">Phòng Tuyển Sinh</span>
                        <a th:href="@{/posts/{id}(id=${post.id})}" class="d-block text-dark text-decoration-none fw-semibold small mb-1" th:text="${post.title}">Đề án tuyển sinh 2026</a>
                        <div class="d-flex justify-content-between text-muted" style="font-size: 0.75rem;">
                            <span th:text="${#temporals.format(post.createdAt, 'dd/MM/yyyy')}">05/09/2026</span>
                            <span th:if="${post.hasVideo}"><i class="bi bi-play-circle-fill text-danger"></i> Video</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
```

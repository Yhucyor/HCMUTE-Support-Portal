# 🎓 GIẢI MÃ SỰ CỐ BÁO LỖI 500 KHI ĐĂNG BÀI: DÀNH CHO NGƯỜI MỚI BẮT ĐẦU HỌC JAVA CORE

> **Chủ đề phân tích:** Vì sao vừa đăng bài xong bấm xem lại bị lỗi trắng trang (HTTP 500)?  
> **Khái niệm Java Core áp dụng:** Tham chiếu bộ nhớ (Heap Reference), Đối tượng Wrapper vs Primitive, Quy ước JavaBeans và Cơ chế Quản lý Vùng nhớ của ORM (Hibernate).  
> **Người viết:** Nhóm Phát Triển Nền Tảng Hỗ Trợ Sinh Viên (Senior Mentor)  
> **Phong cách truyền tải:** Gần gũi, dùng hình ảnh trực quan đời thường, giải thích cặn kẽ từng dòng lệnh cho tân binh học Java Core.

---

## ⚡ TÓM TẮT TRONG 3 DÒNG DỄ NHỚ

1. **Lỗi 1 (Tráo cặp tài liệu bất hợp pháp):** Anh lập trình viên tiện tay ném chiếc cặp quản lý tệp cũ đi và nhét một chiếc cặp mới vào tay Entity, làm "Bác bảo vệ Hibernate" hoảng hốt ném lỗi dừng máy vì sợ mất tài liệu (`orphanRemoval = true`).
2. **Lỗi 2 (So sánh không khí với số 0):** Giao diện cố so sánh một biến chưa có giá trị (`null`) xem có lớn hơn `0` không, khiến máy tính bối rối làm nổ tung trang web (`NullPointerException`).
3. **Cách khắc phục:** Không được tráo cặp của Entity (gửi thẳng danh sách vào DTO cho người xem), và luôn kiểm tra "có đồ hay không" trước khi đem đi so sánh (`att.fileSize != null and att.fileSize > 0`).

---

## 🎒 CÂU CHUYỆN 1: BÁC THỦ KHO HIBERNATE VÀ CHIẾC CẶP BỊ TRÁO ĐỔI

### 1. Kiến thức nền tảng Java Core: Con trỏ tham chiếu (Reference)
Trong Java, khi bạn viết:
```java
List<String> capA = new ArrayList<>();
```
Biến `capA` thực chất **không chứa các món đồ**, mà nó chỉ là một **mẩu giấy ghi địa chỉ** chỉ đến chiếc cặp nằm trong kho bộ nhớ (vùng nhớ Heap).

### 2. Bác thủ kho Hibernate và điều luật `orphanRemoval = true`
Khi bài viết (`Post`) được lưu vào Cơ sở dữ liệu thông qua Hibernate / JPA, trong Entity ta có khai báo:
```java
@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
private List<PostAttachment> attachments = new ArrayList<>();
```

Hãy tưởng tượng:
* `Post` là một **Hồ sơ sinh viên**.
* `attachments` là **Chiếc cặp đựng các tệp đính kèm** của hồ sơ đó.
* `orphanRemoval = true` nghĩa là quy định nghiêm ngặt: *"Nếu có bất kỳ tờ giấy nào bị lôi ra khỏi chiếc cặp này, tờ giấy đó sẽ bị coi là 'mồ côi' (orphan) và lập tức bị bác thủ kho vứt vào máy hủy tài liệu (xóa khỏi Database)!"*

Để bảo vệ quy định này, bác thủ kho Hibernate đã gắn một chiếc **khóa chíp điện tử thông minh** (gọi là Hibernate `PersistentBag`) vào chính chiếc cặp ban đầu.

---

### 3. Điều gì đã xảy ra khiến hệ thống phát nổ? (Bug phân tích)
Sau khi người dùng vừa bấm "Đăng bài", máy chủ lưu bài viết xong thì tự động chuyển hướng đến trang xem chi tiết:
`GET /feed/official/{id}` $\rightarrow$ Gọi hàm `getPostById(id)`.

Đoạn code cũ đã viết như sau:
```java
@Transactional
public PostResponseDto getPostById(Long id) {
    // Bước 1: Bác thủ kho mang hồ sơ Post ra bàn làm việc (Managed Entity)
    Post post = postRepository.findByIdWithDetails(id).orElseThrow();

    // Bước 2: Tự động tăng lượt xem
    post.setViewCount(post.getViewCount() + 1);
    postRepository.save(post);

    // Bước 3: Đi tìm các tệp đính kèm trong Database
    List<PostAttachment> attachments = attachmentRepository.findByPostIdAndIsDeletedFalse(id);

    // ❌❌❌ BƯỚC 4: HÀNH ĐỘNG GÂY TAI HỌA ❌❌❌
    post.setAttachments(attachments); 

    return mapToDto(post);
}
```

Hãy nhìn kỹ **Bước 4**:
* `attachmentRepository.findByPostIdAndIsDeletedFalse(id)` vừa may ra một **chiếc cặp hoàn toàn mới** (`new ArrayList`). Chiếc cặp này KHÔNG CÓ khóa chíp điện tử của bác thủ kho.
* Câu lệnh `post.setAttachments(attachments)` đã **vứt phăng chiếc cặp cũ có khóa chíp** vào sọt rác, và nhét chiếc cặp mới tinh vào tay `post`.

Khi phương thức kết thúc (hết `@Transactional`), bác thủ kho bước vào để kiểm tra và lưu lại dữ liệu:
* Bác thủ kho giật mình: *"Ủa? Chiếc cặp có gắn khóa chíp của tôi đâu mất rồi? Cậu tự ý vứt chiếc cặp cũ đi rồi thay bằng cặp lạ à? Vậy toàn bộ tài liệu trong cặp cũ bị biến thành trẻ mồ côi hết rồi sao? Tôi không biết phải hủy tài liệu nào nữa!"*
* Vì quá bối rối và để ngăn chặn việc xóa nhầm dữ liệu của trường học, Bác thủ kho Hibernate lập tức **kéo còi báo động khẩn cấp**:
  ```
  org.hibernate.HibernateException: A collection with cascade="all-delete-orphan" 
  was cleared and re-referenced by a new collection instance!
  ```
* Tiếng còi báo động này làm máy chủ Tomcat giật mình, không xử lý tiếp được và trả về màn hình **Mã lỗi 500 (Internal Server Error)**!

---

### 4. Cách các lập trình viên Senior sửa chữa
Quy tắc vàng của Hibernate: **Tuyệt đối không được tráo địa chỉ chiếc cặp (`setList`) của một Entity đang được quản lý!**

Ta chỉ muốn lấy danh sách tệp ra để đưa cho người dùng xem trên web, chứ không hề có nhu cầu thay đổi chiếc cặp trong kho! Vì vậy, ta chuyển thẳng danh sách tệp vào DTO (Data Transfer Object - giỏ hàng chuyển phát nhanh):

```java
@Transactional
public PostResponseDto getPostById(Long id) {
    Post post = postRepository.findByIdWithDetails(id).orElseThrow();

    // Tăng lượt xem an toàn
    post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
    postRepository.save(post);

    // Lấy tệp từ DB
    List<PostAttachment> attachments = attachmentRepository.findByPostIdAndIsDeletedFalse(id);

    // ✅ Chuẩn: Đưa thẳng vào giỏ DTO gửi ra ngoài, KHÔNG đụng vào cặp của post
    return mapToDto(post, attachments);
}
```
👉 **Kết quả:** Bác thủ kho Hibernate thở phào nhẹ nhõm, không còn ai tự ý tráo cặp nữa, giao dịch diễn ra êm đềm!

---

## ⚖️ CÂU CHUYỆN 2: PHÉP SO SÁNH "KHÔNG KHÍ VỚI SỐ 0"

### 1. Kiến thức Java Core: `long` (Nguyên thủy) vs `Long` (Đối tượng Wrapper)
* Kiểu nguyên thủy `long`: Luôn luôn có một con số cụ thể, mặc định là `0L`.
* Kiểu đối tượng `Long`: Có thể chứa một con số (`1024L`), nhưng cũng có thể chứa `null` (nghĩa là **không có gì cả - khoảng không trống rỗng**).

### 2. Sự cố trên trang web (View HTML)
Tại giao diện chi tiết thông báo (`official-detail.html`), lập trình viên muốn hiển thị dung lượng của tệp (ví dụ: `1.5 MB`):
```html
<!-- DÒNG LỆNH CŨ GÂY LỖI -->
<small th:text="${att.fileSize > 0 ? att.fileSize + ' MB' : att.fileType}">1.2 MB</small>
```

Điều gì xảy ra nếu một tệp tải lên mà hệ thống chưa kịp ghi nhận dung lượng (`att.fileSize == null`)?
* Biểu thức trở thành: `null > 0`.
* Trình thông dịch Thymeleaf cố gắng biến `null` thành số để so sánh với số 0.
* Máy tính kêu lên: *"Làm sao tôi biết một 'khoảng không vô hình' có lớn hơn số 0 hay không?!"*
* Kết quả: Ném lỗi `NullPointerException` (hoặc `SpelEvaluationException`), làm trang HTML đang render dở bị ngắt ngang $\rightarrow$ **Lỗi 500 trắng trang!**

### 3. Cách sửa chuẩn lập trình viên:
Trước khi đem đồ đi cân ký, phải kiểm tra xem **trong tay có đồ thật hay không** (`!= null`):
```html
<!-- DÒNG LỆNH ĐÃ SỬA AN TOÀN TUYỆT ĐỐI -->
<small th:text="${att.fileSize != null and att.fileSize > 0 ? #numbers.formatDecimal(att.fileSize / 1024.0 / 1024.0, 1, 2) + ' MB' : 'Tệp đính kèm'}">1.2 MB</small>
```

---

## 🗣️ CÂU CHUYỆN 3: HỎI SAI TÊN KHIẾN HỆ THỐNG ĐỨNG HÌNH

### 1. Kiến thức Java Core: Chuẩn JavaBeans (Getter / Setter)
Trong Java, khi bạn tạo một thuộc tính boolean:
```java
private boolean youtubeVideo;
```
Quy ước đặt tên (JavaBeans Naming Convention) bắt buộc phương thức lấy dữ liệu phải có tên là:
* `isYoutubeVideo()` hoặc `getYoutubeVideo()`.

Khi Thymeleaf chạy trên giao diện web, nó đọc cú pháp `${post.youtubeVideo}` bằng cách tự động đi tìm hàm `getYoutubeVideo()` hoặc `isYoutubeVideo()`.

### 2. Lỗi `post.videoUrl.blank`
Trong code cũ có dòng:
```html
<div th:if="${post.videoUrl != null and !post.videoUrl.blank}">
```
* Đối tượng `post.videoUrl` là một chuỗi ký tự (`java.lang.String`).
* Dù trong Java 11+ có phương thức `isBlank()`, nhưng lớp `String` không phải là một JavaBean tiêu chuẩn.
* Bộ máy đọc Thymeleaf (SpEL) đi tìm kiếm thuộc tính tên là `getBlank()` trên chuỗi `String` nhưng không thấy $\rightarrow$ Báo lỗi `Property or field 'blank' cannot be found`.

### 3. Cách sửa:
Sử dụng công cụ tiện ích chuẩn của Thymeleaf:
```html
<div th:if="${post.videoUrl != null and !#strings.isEmpty(post.videoUrl)}">
```
Và bổ sung đầy đủ cặp hàm `getYoutubeVideo()`, `getDriveVideo()`, `getDirectVideo()` trong DTO.

---

## 🏆 4 BÀI HỌC VÀNG CHO NGƯỜI MỚI HỌC JAVA & SPRING BOOT

| STT | Bài học đắt giá | Lý do kỹ thuật |
| :--- | :--- | :--- |
| **1** | **Không bao giờ gọi `entity.setList(newList)`** trên quan hệ `@OneToMany(orphanRemoval = true)` | Hibernate quản lý vòng đời collection bằng Proxy. Tráo tham chiếu mới sẽ làm đứt gãy cơ chế theo dõi "tài liệu mồ côi". |
| **2** | **Tách biệt Entity và DTO** | Entity chỉ dùng để làm việc với Database trong Service. Dữ liệu mang ra cho Controller/View hiển thị thì đóng gói vào DTO. |
| **3** | **Cảnh giác cao độ với `null` khi so sánh** | Với kiểu Wrapper (`Long`, `Integer`), luôn kiểm tra `x != null` trước khi làm phép tính toán `x > 0`. |
| **4** | **Tuân thủ chặt chẽ chuẩn JavaBeans** | Đặt tên hàm Getter/Setter chuẩn chỉ (`getYoutubeVideo()`, `isYoutubeVideo()`) để các framework như Spring, Jackson, Thymeleaf tự động bắt nhịp mượt mà. |

---
*Tài liệu này được lưu trữ tại `docs/analysis/post_create_500_error_analysis.md` phục vụ đào tạo tân binh và tra cứu sự cố hệ thống.*

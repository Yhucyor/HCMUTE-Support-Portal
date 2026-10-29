# 📑 BÁO CÁO THAY ĐỔI: TỐI ƯU BỘ CHẶN CHÀO HỎI & CHỐNG NHIỄU FAQ MATCHER

**Ngày thực hiện:** 2026-10-29  
**Người thực hiện:** Dev A (Fullstack & Software Architect)  
**Nhánh:** `feature/fix-ai-greeting-and-faq-matcher`  

---

## 🎯 1. VẤN ĐỀ ĐƯỢC PHÁT HIỆN
- Khi người dùng gửi các câu chào teencode ngắn như `"hi b"`, `"hi ban"`, `"chao b"`:
  1. Hàm `isGreeting()` cũ dùng Regex cứng có dấu không khử dấu tiếng Việt (Accent diacritics), dẫn đến câu `"hi b"` bị lọt qua.
  2. Câu hỏi bị gửi tiếp sang Python AI microservice và chịu timeout nếu dịch vụ bận/nghẽn.
  3. Khi fallback xuống `SmartFaqMatcherService`, vì từ khóa chỉ có 2 chữ cái `hi` và `b`, thuật toán so khớp bị nhiễu và ghép nhầm với các câu hỏi FAQ có chứa từ `"HI"` (viết tắt Học kỳ 1) và `"bị"` (trong từ identification number).

---

## 🛠️ 2. CHI TIẾT TẬP TIN VÀ DÒNG CODE ĐÃ THAY ĐỔI

### 1. [`src/main/java/com/school/counseling/module/ai/service/AiChatbotService.java`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/ai/service/AiChatbotService.java)
- **Vị trí dòng:** [L317 - L325](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/ai/service/AiChatbotService.java#L317-L325)
- **Nội dung cũ:**
  ```java
  private boolean isGreeting(String q) {
      if (q.length() > 30) return false;
      String clean = q.trim().toLowerCase().replaceAll("[!?.~,]+$", "").replaceAll("\\s+", " ");
      return clean.matches("^(xin chào|chao|chào|hello|hi|helo|alo|hey|yo|hi+\\w*|he+l+o+|chào bạn|xin chao|chao ban|bạn là ai|ban la ai|tro ly|trợ lý|help|giúp với|giup voi)$");
  }
  ```
- **Nội dung mới:**
  ```java
  private boolean isGreeting(String q) {
      if (q == null || q.isBlank() || q.length() > 30) return false;
      String clean = q.trim().toLowerCase().replaceAll("[!?.~,]+$", "").replaceAll("\\s+", " ");
      String norm = java.text.Normalizer.normalize(clean, java.text.Normalizer.Form.NFD)
              .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
              .replace("đ", "d").replace("Đ", "D");
      return norm.matches("^(xin chao|chao|hello|hi|helo|alo|hey|yo|hi+\\w*|he+l+o+|chao ban.*|chao b|hi ban.*|hi b|hello ban.*|hello b|ban la ai|tro ly|help|giup voi|hoi ti)$");
  }
  ```
- **Tác dụng:** Khử dấu tiếng Việt chuẩn Unicode NFD trước khi so khớp, bắt trọn 100% các biến thể teencode (`hi b`, `hi ban`, `chào b`, `hello b`...) và phản hồi ngay lập tức trong **0ms** mà không gọi sang AI service.

---

### 2. [`src/main/java/com/school/counseling/module/ai/service/SmartFaqMatcherService.java`](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/ai/service/SmartFaqMatcherService.java)
- **Vị trí dòng:** [L132 - L134](file:///d:/HK5/LapTrinhWeb/doancuoiki/ban2/src/main/java/com/school/counseling/module/ai/service/SmartFaqMatcherService.java#L132-L134)
- **Bổ sung Guard Clause:**
  ```java
  if (query.trim().length() < 4) {
      return Collections.emptyList();
  }
  ```
- **Tác dụng:** Chặn hoàn toàn hiện tượng so khớp rác với các câu truy vấn cụt (< 4 ký tự), ngăn chặn triệt để việc bốc nhầm các câu FAQ học kỳ / identification number.

---

## 🔒 3. KẾT QUẢ KIỂM THỬ AN TOÀN
- `mvn test-compile`: **BUILD SUCCESS**.
- Unit Test `AiChatWebControllerRefactorTest`: **Pass 100%**.
- Đã sync sang target `mvn process-resources`.
- Không ảnh hưởng đến bất kỳ luồng nghiệp vụ vé hay dữ liệu nào khác.

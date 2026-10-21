package com.school.counseling.module.ai.service;

import com.school.counseling.module.ai.dto.AiChatRequest;
import com.school.counseling.module.ai.dto.AiChatResponse;
import com.school.counseling.module.ai.service.SmartFaqMatcherService.FaqMatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiChatbotService {

    private static final Logger log = LoggerFactory.getLogger(AiChatbotService.class);

    @Value("${ai.chatbot.url:http://localhost:8000/api/chat}")
    private String chatbotApiUrl;

    private final RestTemplate restTemplate;
    private final SmartFaqMatcherService smartFaqMatcherService;

    public AiChatbotService(RestTemplate restTemplate, SmartFaqMatcherService smartFaqMatcherService) {
        this.restTemplate = restTemplate;
        this.smartFaqMatcherService = smartFaqMatcherService;
    }

    public AiChatResponse getChatResponse(String query, String sessionId) {
        return getChatResponse(query, sessionId, "gemini");
    }

    public AiChatResponse getChatResponse(String query, String sessionId, String provider) {
        if (query == null || query.trim().isEmpty()) {
            AiChatResponse emptyResp = new AiChatResponse();
            emptyResp.setAnswer("Xin chào! Bạn có thể đặt câu hỏi về tuyển sinh, học vụ, học phí hoặc quy trình của HCMUTE để mình giải đáp nhé.");
            emptyResp.setSessionId(sessionId);
            emptyResp.setProvider(provider != null ? provider : "qaute-ai");
            return emptyResp;
        }

        String cleanQuery = query.trim();
        String lowerQuery = cleanQuery.toLowerCase();

        // 1. Phản hồi nhanh cho câu chào hỏi thân thiện
        if (isGreeting(lowerQuery)) {
            AiChatResponse greetingResp = new AiChatResponse();
            greetingResp.setAnswer("Xin chào bạn! 👋 Tôi là Trợ lý ảo AI QAUTE của Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE).\n\n"
                    + "Tôi có thể hỗ trợ bạn nhanh chóng về:\n"
                    + "• 🎓 Thông tin tuyển sinh, xét tuyển học bạ, điểm thi ĐGNL 2026\n"
                    + "• 📚 Đăng ký môn học, học phí, chuẩn đầu ra Tiếng Anh (AVDV, AVDR)\n"
                    + "• 💻 Hướng dẫn đồng bộ dữ liệu học vụ & xem điểm Portal\n"
                    + "• 🎫 Hướng dẫn tạo Ticket gửi yêu cầu tới các Phòng/Ban với cam kết SLA\n\n"
                    + "Bạn cần mình giải đáp hoặc tra cứu vấn đề gì hôm nay?");
            greetingResp.setSessionId(sessionId);
            greetingResp.setProvider("qaute-assistant");
            return greetingResp;
        }

        // 2. Nhận diện các nghiệp vụ trọng tâm của Portal & Nhà trường
        if (lowerQuery.contains("ticket") || (lowerQuery.contains("gửi") && lowerQuery.contains("yêu cầu")) || lowerQuery.contains("sla")) {
            AiChatResponse ticketResp = new AiChatResponse();
            ticketResp.setAnswer("🎫 **Quy trình gửi yêu cầu hỗ trợ (Ticket SLA) trên HCMUTE Portal:**\n\n"
                    + "1. **Bước 1:** Đăng nhập tài khoản sinh viên/giảng viên tại mục [Đăng nhập](/auth/login).\n"
                    + "2. **Bước 2:** Nhấn nút **'Gửi yêu cầu'** trên thanh điều hướng hoặc tại Bảng điều khiển (Dashboard).\n"
                    + "3. **Bước 3:** Chọn đúng **Khoa / Phòng / Ban** tiếp nhận (Phòng Đào tạo, Phòng Tuyển sinh & CTSV, Phòng Kế toán...), nhập tiêu đề và mô tả chi tiết vấn đề kèm tệp đính kèm (nếu có).\n"
                    + "4. **Bước 4:** Chọn mức độ ưu tiên. Hệ thống sẽ tự động kích hoạt **Thời hạn cam kết xử lý (SLA)**:\n"
                    + "   • 🔴 **Khẩn cấp (URGENT):** Phản hồi trong vòng **24 giờ**.\n"
                    + "   • 🟠 **Cao (HIGH):** Phản hồi trong vòng **48 giờ**.\n"
                    + "   • 🟡 **Trung bình (MEDIUM):** Phản hồi trong vòng **72 giờ**.\n"
                    + "   • 🟢 **Thấp (LOW):** Phản hồi trong vòng **7 ngày**.\n"
                    + "5. **Bước 5:** Bạn sẽ nhận được thông báo qua Email và Portal khi Thầy/Cô phụ trách phản hồi Ticket.");
            ticketResp.setSessionId(sessionId);
            ticketResp.setProvider("hcmute-ticket-service");
            return ticketResp;
        }

        if ((lowerQuery.contains("đồng bộ") && lowerQuery.contains("học vụ")) 
                || (lowerQuery.contains("xem") && lowerQuery.contains("điểm")) 
                || lowerQuery.contains("portal")) {
            AiChatResponse portalResp = new AiChatResponse();
            portalResp.setAnswer("💻 **Hướng dẫn đồng bộ học vụ và xem điểm trên HCMUTE Portal:**\n\n"
                    + "1. **Đồng bộ học vụ tự động:** Đăng nhập Portal, truy cập mục **Học vụ / Xem điểm**, nhấn nút **'Đồng bộ dữ liệu'** để hệ thống kết nối trực tiếp với Cổng đào tạo Nhà trường và cập nhật:\n"
                    + "   • Toàn bộ bảng điểm các học kỳ, điểm trung bình tích lũy (GPA / CPA).\n"
                    + "   • Thời khóa biểu tuần và lịch thi chính thức.\n"
                    + "   • Tiến độ tích lũy tín chỉ và cảnh báo học vụ (nếu có).\n"
                    + "2. **Hỗ trợ kỹ thuật:** Nếu tài khoản Portal gặp lỗi đồng bộ hoặc không hiển thị điểm, bạn có thể tạo Ticket gửi trực tiếp tới Bộ phận Quản trị Kỹ thuật để được hỗ trợ nhanh nhất.");
            portalResp.setSessionId(sessionId);
            portalResp.setProvider("hcmute-portal-service");
            return portalResp;
        }

        if ((lowerQuery.contains("tuyển sinh") && (lowerQuery.contains("2026") || lowerQuery.contains("đại học")))
                || lowerQuery.contains("phương thức tuyển sinh")) {
            AiChatResponse tsResp = new AiChatResponse();
            tsResp.setAnswer("🎓 **Thông tin Tuyển sinh Đại học Chính quy HCMUTE 2026:**\n\n"
                    + "Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE) áp dụng các phương thức xét tuyển chính:\n"
                    + "1. **Phương thức 1:** Xét tuyển thẳng, ưu tiên xét tuyển theo quy chế của Bộ GD&ĐT.\n"
                    + "2. **Phương thức 2:** Xét tuyển dựa trên kết quả học bạ THPT (điểm trung bình các học kỳ theo tổ hợp môn).\n"
                    + "3. **Phương thức 3:** Xét tuyển dựa trên kết quả kỳ thi Đánh giá năng lực của ĐHQG-HCM năm 2026.\n"
                    + "4. **Phương thức 4:** Xét tuyển dựa trên điểm thi Tốt nghiệp THPT 2026.\n"
                    + "5. **Phương thức 5:** Xét tuyển kết hợp chứng chỉ Tiếng Anh quốc tế (IELTS, TOEFL...) và học bạ THPT.\n\n"
                    + "📍 *Chi tiết đề án và chỉ tiêu từng ngành:* Xem tại website chính thức https://tuyensinh.hcmute.edu.vn hoặc Hotline Tuyển sinh: **0379 668 768** / **(028) 3896 1333**.");
            tsResp.setSessionId(sessionId);
            tsResp.setProvider("hcmute-admission-service");
            return tsResp;
        }

        if ((lowerQuery.contains("ngành") && lowerQuery.contains("học phí")) || lowerQuery.contains("tra cứu ngành")) {
            AiChatResponse feeResp = new AiChatResponse();
            feeResp.setAnswer("🔍 **Tra cứu Ngành đào tạo & Học phí HCMUTE:**\n\n"
                    + "Trường ĐH Sư phạm Kỹ thuật TP.HCM hiện đào tạo hơn 50 ngành đại học chính quy thuộc các khối: Công nghệ thông tin, Cơ khí, Điện - Điện tử, Tự động hóa, Kinh tế, Ngoại ngữ, May & Thời trang...\n\n"
                    + "💰 **Mức học phí tham khảo (theo tín chỉ):**\n"
                    + "• Chương trình Chuẩn: dao động khoảng **15 - 19 triệu đồng / học kỳ** tùy số lượng tín chỉ đăng ký.\n"
                    + "• Chương trình Chất lượng cao / Tiếng Anh: dao động khoảng **22 - 28 triệu đồng / học kỳ**.\n"
                    + "• Nhà trường có chính sách học bổng khuyến khích học tập từ 50% - 100% học phí dành cho sinh viên có thành tích xuất sắc và các học bổng tài trợ doanh nghiệp.\n\n"
                    + "🏛️ *Phòng phụ trách:* Phòng Đào tạo (A1-101) & Phòng Tuyển sinh & CTSV (A1-203).");
            feeResp.setSessionId(sessionId);
            feeResp.setProvider("hcmute-academic-service");
            return feeResp;
        }

        // 3. Tra cứu cơ sở tri thức FAQ HCMUTE (349 câu hỏi học vụ chính thức)
        List<FaqMatchResult> faqMatches = Collections.emptyList();
        try {
            faqMatches = smartFaqMatcherService.matchQuestion(cleanQuery, null);
        } catch (Exception e) {
            log.warn("Lỗi khi tra cứu SmartFaqMatcherService: {}", e.getMessage());
        }

        // Nếu khớp FAQ với độ tin cậy tốt (score >= 0.25)
        if (!faqMatches.isEmpty() && faqMatches.get(0).confidenceScore() >= 0.25) {
            FaqMatchResult top = faqMatches.get(0);
            StringBuilder sb = new StringBuilder();
            sb.append(top.answer().trim());

            sb.append("\n\n🏛️ **Đơn vị phụ trách:** ").append(top.departmentName());
            if (top.category() != null && !top.category().isBlank()) {
                sb.append(" • *").append(top.category()).append("*");
            }

            // Gợi ý câu hỏi liên quan nếu có
            if (faqMatches.size() > 1) {
                List<String> relatedList = new ArrayList<>();
                for (int i = 1; i < faqMatches.size() && relatedList.size() < 2; i++) {
                    String q = faqMatches.get(i).question().trim();
                    if (!q.equalsIgnoreCase(top.question()) && !relatedList.contains(q)) {
                        relatedList.add(q);
                    }
                }
                if (!relatedList.isEmpty()) {
                    sb.append("\n\n💡 **Có thể bạn quan tâm:**");
                    for (String r : relatedList) {
                        sb.append("\n👉 ").append(r);
                    }
                }
            }

            AiChatResponse faqResp = new AiChatResponse();
            faqResp.setAnswer(sb.toString());
            faqResp.setSessionId(sessionId);
            faqResp.setProvider("hcmute-faq-knowledge");
            List<Object> sources = new ArrayList<>();
            sources.add(Map.of(
                    "title", top.question(),
                    "department", top.departmentName(),
                    "category", top.category() != null ? top.category() : "Học vụ",
                    "score", top.confidenceScore()
            ));
            faqResp.setSources(new ArrayList<>()); // Empty list thay vì null
            return faqResp;
        }

        // 3. Nếu chưa khớp FAQ rõ ràng, thử gọi sang Python Microservice RAG (nếu đang bật)
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            AiChatRequest requestPayload = AiChatRequest.builder()
                .query(cleanQuery)
                .sessionId(sessionId)
                .provider(provider != null ? provider : "gemini")
                .build();
            HttpEntity<AiChatRequest> requestEntity = new HttpEntity<>(requestPayload, headers);

            ResponseEntity<AiChatResponse> responseEntity = restTemplate.postForEntity(
                    chatbotApiUrl,
                    requestEntity,
                    AiChatResponse.class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                AiChatResponse body = responseEntity.getBody();
                if (body.getAnswer() != null && !body.getAnswer().isBlank()
                        && !body.getAnswer().contains("Xin lỗi, hệ thống AI hiện không thể")) {
                    return body;
                }
            }
        } catch (Exception e) {
            log.debug("Python AI microservice tại {} không phản hồi ({}), dùng bộ hỗ trợ học vụ.", chatbotApiUrl, e.getMessage());
        }

        // 4. Fallback thông minh: Nếu có FAQ kết quả gần nhất dù điểm thấp hơn, dùng kết quả đó
        if (!faqMatches.isEmpty()) {
            FaqMatchResult top = faqMatches.get(0);
            StringBuilder sb = new StringBuilder();
            sb.append("Dưới đây là thông tin học vụ gần nhất với câu hỏi của bạn:\n\n");
            sb.append(top.answer().trim());
            sb.append("\n\n🏛️ **Đơn vị phụ trách:** ").append(top.departmentName());

            AiChatResponse fallbackFaq = new AiChatResponse();
            fallbackFaq.setAnswer(sb.toString());
            fallbackFaq.setSessionId(sessionId);
            fallbackFaq.setProvider("hcmute-faq-fallback");
            return fallbackFaq;
        }

        // 5. Fallback hướng dẫn quy trình học vụ & Ticket SLA
        AiChatResponse guideResp = new AiChatResponse();
        guideResp.setAnswer("Trợ lý QAUTE đã ghi nhận câu hỏi của bạn: *\"" + cleanQuery + "\"*.\n\n"
                + "Hiện tại câu hỏi này cần kiểm tra hồ sơ sinh viên cụ thể hoặc nghiệp vụ chuyên sâu. Bạn có thể:\n\n"
                + "1. 🎫 **Gửi yêu cầu hỗ trợ (Ticket):** Đăng nhập Portal và vào mục **'Gửi yêu cầu'** để tạo phiếu hỗ trợ đến đúng Khoa/Phòng phụ trách (cam kết phản hồi theo thời hạn SLA).\n"
                + "2. 🔍 **Tra cứu FAQ:** Mở trang **'Hỏi đáp (FAQ)'** trên menu chính để tìm kiếm theo từng Phòng/Khoa.\n"
                + "3. 📞 **Liên hệ trực tiếp:**\n"
                + "   • Phòng Đào tạo (A1-101): (028) 3896 8641\n"
                + "   • Phòng Tuyển sinh & CTSV (A1-203): (028) 3896 1333 - Hotline: 0379 668 768");
        guideResp.setSessionId(sessionId);
        guideResp.setProvider("hcmute-counseling-guide");
        return guideResp;
    }

    private boolean isGreeting(String q) {
        if (q.length() > 25) return false;
        return q.matches("^(xin chào|chao|chào|hello|hi|alo|helo|chào bạn|xin chao|chao ban|bạn là ai|ban la ai|tro ly|trợ lý|help|giúp với|giup voi) *[!?.~]*$");
    }
}

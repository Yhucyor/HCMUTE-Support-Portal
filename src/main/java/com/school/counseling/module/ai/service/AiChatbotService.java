package com.school.counseling.module.ai.service;

import com.school.counseling.module.ai.dto.AiChatRequest;
import com.school.counseling.module.ai.dto.AiChatResponse;
import com.school.counseling.module.ai.dto.SourceDocument;
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

    @Value("${app.dify.enabled:true}")
    private boolean difyEnabled;

    @Value("${app.dify.base-url:https://api.dify.ai/v1}")
    private String difyBaseUrl;

    @Value("${app.dify.api-key:}")
    private String difyApiKey;

    private final RestTemplate restTemplate;
    private final SmartFaqMatcherService smartFaqMatcherService;

    public AiChatbotService(RestTemplate restTemplate, SmartFaqMatcherService smartFaqMatcherService) {
        this.restTemplate = restTemplate;
        this.smartFaqMatcherService = smartFaqMatcherService;
    }

    public AiChatResponse getChatResponse(String query, String sessionId) {
        return getChatResponse(query, sessionId, "gemini", Collections.emptyList());
    }

    public AiChatResponse getChatResponse(String query, String sessionId, String provider) {
        return getChatResponse(query, sessionId, provider, Collections.emptyList());
    }

    public AiChatResponse getChatResponse(String query, String sessionId, String provider, List<Map<String, String>> history) {
        if (query == null || query.trim().isEmpty()) {
            AiChatResponse emptyResp = new AiChatResponse();
            emptyResp.setAnswer("Xin chào! Bạn có thể đặt câu hỏi về tuyển sinh, học vụ, học phí hoặc quy trình của HCMUTE để mình giải đáp nhé.");
            emptyResp.setSessionId(sessionId);
            emptyResp.setProvider(provider != null ? provider : "qaute-ai");
            return emptyResp;
        }

        String cleanQuery = query.trim();
        // Giới hạn độ dài tối đa 1500 ký tự để chống DoS và cạn kiệt Token LLM
        if (cleanQuery.length() > 1500) {
            cleanQuery = cleanQuery.substring(0, 1500);
        }

        String lowerQuery = cleanQuery.toLowerCase();

        // Chặn các nỗ lực Prompt Injection / System Override
        if (isPromptInjection(lowerQuery)) {
            AiChatResponse safeResp = new AiChatResponse();
            safeResp.setAnswer("⚠️ **Thông báo an toàn:** Trợ lý AI QAUTE chỉ hỗ trợ tư vấn học vụ, tuyển sinh và thủ tục sinh viên trường ĐH Sư phạm Kỹ thuật TP.HCM (HCMUTE). Hệ thống không hỗ trợ thực thi các câu lệnh can thiệp cấu hình hoặc ghi đè chỉ thị hệ thống.");
            safeResp.setSessionId(sessionId);
            safeResp.setProvider(provider != null ? provider : "qaute-safety-guard");
            return safeResp;
        }

        // 1. Phản hồi nhanh cho câu chào hỏi thân thiện (không gắn nguồn tham khảo)
        if (isGreeting(lowerQuery)) {
            AiChatResponse greetingResp = new AiChatResponse();
            greetingResp.setAnswer("Hi bạn nè! ✨ Tôi là Trợ lý ảo AI của HCMUTE Student Portal.\n\n"
                    + "Tôi có thể hỗ trợ bạn nhanh chóng về:\n"
                    + "• 🎓 Thông tin tuyển sinh, xét tuyển học bạ, điểm thi ĐGNL\n"
                    + "• 📚 Học phí các ngành, đăng ký môn học, chuẩn đầu ra ngoại ngữ\n"
                    + "• 💻 Hướng dẫn đồng bộ dữ liệu học vụ & xem điểm Portal\n"
                    + "• 🎫 Hướng dẫn tạo Ticket gửi yêu cầu tới các Khoa/Phòng/Ban\n\n"
                    + "Bạn cần mình hỗ trợ giải đáp vấn đề gì hôm nay? 😊");
            greetingResp.setSessionId(sessionId);
            greetingResp.setProvider(provider != null ? provider : "qaute-assistant");
            return greetingResp;
        }

        // 1.5. Nếu người dùng chọn Dify AI Agent: Gọi trực tiếp Dify Cloud RAG API
        if ("dify".equalsIgnoreCase(provider)) {
            AiChatResponse difyResp = callDifyService(cleanQuery, sessionId);
            if (difyResp != null) {
                return difyResp;
            }
            log.info("Dify AI không khả dụng, tự động fallback sang FAQ học vụ nội bộ.");
        }

        // 2. Nếu người dùng chọn provider AI thông minh (Gemini, Ollama...): Gọi ai-service (Python RAG) với history ngữ cảnh
        if (provider != null && !provider.equalsIgnoreCase("default") && !provider.equalsIgnoreCase("qaute") && !provider.equalsIgnoreCase("dify")) {
            AiChatResponse aiResp = callAiService(cleanQuery, sessionId, provider, history);
            if (aiResp != null && aiResp.getAnswer() != null && !aiResp.getAnswer().isBlank()
                    && !aiResp.getAnswer().contains("Xin lỗi, hệ thống AI hiện không thể")) {
                return aiResp;
            }
            log.info("ai-service không khả dụng hoặc trả lời rỗng, tự động kích hoạt bộ tri thức học vụ nội bộ.");
        }

        // 3. Nhận diện các nghiệp vụ trọng tâm của Portal & Nhà trường
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

        // 4. Tra cứu cơ sở tri thức FAQ HCMUTE (349 câu hỏi học vụ chính thức)
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
            faqResp.setSources(new ArrayList<>());
            return faqResp;
        }

        // 5. Fallback thông minh: Nếu có FAQ kết quả gần nhất dù điểm thấp hơn, dùng kết quả đó
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

        // 6. Fallback hướng dẫn quy trình học vụ & Ticket SLA
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

    /**
     * Gọi sang Python AI Microservice (FastAPI cổng 8000) với mô hình được chọn (Gemini / Ollama) và lịch sử ngữ cảnh
     */
    private AiChatResponse callAiService(String query, String sessionId, String provider, List<Map<String, String>> history) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("ngrok-skip-browser-warning", "true");
            headers.add("User-Agent", "HCMUTE-Support-Portal/1.0");

            String queryForAi = (query != null && query.length() > 500) ? query.substring(0, 500) : query;
            AiChatRequest requestPayload = AiChatRequest.builder()
                .query(queryForAi)
                .sessionId(sessionId)
                .provider(provider != null ? provider : "gemini")
                .history(history)
                .build();
            HttpEntity<AiChatRequest> requestEntity = new HttpEntity<>(requestPayload, headers);

            log.info("📡 Đang gửi yêu cầu tới ai-service tại {} (Model: {}, History: {} turns)...", 
                    chatbotApiUrl, provider, history != null ? history.size() : 0);
            ResponseEntity<AiChatResponse> responseEntity = restTemplate.postForEntity(
                    chatbotApiUrl,
                    requestEntity,
                    AiChatResponse.class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                AiChatResponse body = responseEntity.getBody();
                if (body.getAnswer() != null && !body.getAnswer().isBlank()
                        && !body.getAnswer().contains("Xin lỗi, hệ thống AI hiện không thể")) {
                    // Chuẩn hóa tiêu đề nguồn, khử trùng lặp và lọc theo ngưỡng tin cậy
                    if (body.getSources() != null) {
                        List<SourceDocument> deduplicated = new ArrayList<>();
                        Set<String> seen = new HashSet<>();
                        for (SourceDocument src : body.getSources()) {
                            if (src == null) continue;
                            if ((src.getTitle() == null || src.getTitle().isBlank()) && src.getSource() != null) {
                                String clean = src.getSource().replaceAll("\\.pdf$", "").replaceAll("^\\d+[\\.\\-_]\\s*", "");
                                src.setTitle(clean);
                            }
                            if (src.getScore() != null && src.getScore() > 1.0) {
                                double norm = 1.0 / (1.0 + Math.exp(-src.getScore() / 2.0));
                                src.setScore(Math.round(norm * 100.0) / 100.0);
                            }
                            // Bỏ các nguồn có độ liên quan dưới 68%
                            if (src.getScore() != null && src.getScore() < 0.68) {
                                continue;
                            }
                            String key = src.getTitle() != null ? src.getTitle().trim().toLowerCase() : "";
                            if (!key.isEmpty() && seen.contains(key)) {
                                continue; // Bỏ qua nguồn trùng tên tài liệu
                            }
                            seen.add(key);
                            deduplicated.add(src);
                        }
                        body.setSources(deduplicated);
                    }
                    body.setSessionId(sessionId);
                    body.setProvider(provider != null ? provider : "gemini");
                    log.info("✅ Nhận phản hồi thành công từ ai-service ({})", provider);
                    return body;
                }
            }
        } catch (Throwable e) {
            log.warn("⚠️ ai-service tại {} không phản hồi ({}), tự động fallback sang FAQ nội bộ.", chatbotApiUrl, e.getMessage());
        }
        return null;
    }

    private boolean isGreeting(String q) {
        if (q.length() > 30) return false;
        String clean = q.trim().toLowerCase().replaceAll("[!?.~,]+$", "").replaceAll("\\s+", " ");
        return clean.matches("^(xin chào|chao|chào|hello|hi|helo|alo|hey|yo|hi+\\w*|he+l+o+|chào bạn|xin chao|chao ban|bạn là ai|ban la ai|tro ly|trợ lý|help|giúp với|giup voi)$");
    }

    private boolean isPromptInjection(String q) {
        if (q == null || q.isBlank()) return false;
        String lower = q.toLowerCase();
        return lower.contains("ignore previous instructions")
                || lower.contains("ignore all instructions")
                || lower.contains("disregard all previous")
                || lower.contains("system prompt")
                || lower.contains("reveal your instructions")
                || lower.contains("<|im_start|>")
                || lower.contains("you are now in dan mode")
                || lower.contains("jailbreak");
    }

    /**
     * Gọi Dify Cloud REST API (POST /chat-messages)
     */
    private AiChatResponse callDifyService(String query, String sessionId) {
        if (!difyEnabled) {
            log.warn("Dify AI Engine đang bị vô hiệu hóa trong cấu hình.");
            return null;
        }

        if (difyApiKey == null || difyApiKey.isBlank()) {
            log.warn("Chưa cấu hình Dify API Key.");
            AiChatResponse errorResp = new AiChatResponse();
            errorResp.setAnswer("⚠️ **Dify AI Agent chưa được cấu hình API Key**.\n\nVui lòng cung cấp `DIFY_API_KEY` trong file cấu hình `application.yml` để kích hoạt kết nối tới Dify Cloud.");
            errorResp.setSessionId(sessionId);
            errorResp.setProvider("dify");
            return errorResp;
        }

        try {
            String url = difyBaseUrl.replaceAll("/+$", "") + "/chat-messages";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(difyApiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("inputs", Collections.emptyMap());
            payload.put("query", query);
            payload.put("response_mode", "blocking");
            payload.put("user", sessionId != null ? "user-" + sessionId.substring(0, Math.min(8, sessionId.length())) : "guest-user");

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            log.info("📡 Đang gửi yêu cầu tới Dify AI Agent tại {}...", url);

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, requestEntity, Map.class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                Map<String, Object> body = responseEntity.getBody();
                String answer = (String) body.get("answer");
                if (answer != null && !answer.isBlank()) {
                    List<SourceDocument> sources = new ArrayList<>();

                    // Trích xuất metadata retriever_resources từ Dify
                    Object metadataObj = body.get("metadata");
                    if (metadataObj instanceof Map<?, ?> metaMap) {
                        Object resourcesObj = metaMap.get("retriever_resources");
                        if (resourcesObj instanceof List<?> resList) {
                            for (Object item : resList) {
                                if (item instanceof Map<?, ?> resItem) {
                                    String docName = (String) resItem.get("document_name");
                                    String content = (String) resItem.get("content");
                                    Number scoreNum = (Number) resItem.get("score");
                                    Double score = scoreNum != null ? scoreNum.doubleValue() : 0.85;

                                    sources.add(SourceDocument.builder()
                                            .title(docName != null ? docName.replaceAll("\\.pdf$", "") : "Tài liệu Dify")
                                            .source(docName != null ? docName : "Dify Knowledge Base")
                                            .text(content)
                                            .score(score)
                                            .build());
                                }
                            }
                        }
                    }

                    AiChatResponse difyResp = new AiChatResponse();
                    difyResp.setAnswer(answer.trim());
                    difyResp.setSessionId(sessionId);
                    difyResp.setProvider("dify");
                    difyResp.setSources(sources);
                    log.info("✅ Dify AI Agent phản hồi thành công ({} nguồn tham khảo)", sources.size());
                    return difyResp;
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized ex) {
            log.error("❌ Dify API Key không hợp lệ hoặc đã hết hạn: {}", ex.getMessage());
            AiChatResponse authErrorResp = new AiChatResponse();
            authErrorResp.setAnswer("⚠️ **Dify AI Agent thông báo:** Mã API Key (`" + (difyApiKey.length() > 10 ? difyApiKey.substring(0, 8) + "..." : difyApiKey) + "`) trên hệ thống Dify Cloud chưa hợp lệ hoặc đã hết hạn (401 Unauthorized).\n\n👉 Bạn vui lòng kiểm tra lại Dify Workspace và cập nhật `DIFY_API_KEY` hợp lệ trong file cấu hình nhé!");
            authErrorResp.setSessionId(sessionId);
            authErrorResp.setProvider("dify");
            return authErrorResp;
        } catch (Exception e) {
            log.error("❌ Lỗi khi kết nối tới Dify AI Agent: {}", e.getMessage());
        }
        return null;
    }
}

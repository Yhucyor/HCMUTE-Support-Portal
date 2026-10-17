package com.school.counseling.module.ai.service;

import com.school.counseling.module.ai.entity.Faq;
import com.school.counseling.module.ai.repository.FaqRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SERVICE ĐỐI SOÁT NGỮ NGHĨA FAQ NHANH (SMART FAQ MATCHER)
 * 
 * Sử dụng thuật toán Jaccard + Bigram + Academic Synonyms + In-Memory Fast Cache (< 5ms)
 * kết hợp bộ chuẩn hóa viết tắt teencode HCMUTE AcademicAbbreviationUtils.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartFaqMatcherService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FaqRepository faqRepository;

    // Bộ nhớ Cache RAM phục vụ tìm kiếm siêu tốc (< 5ms)
    private final List<FaqCacheItem> faqCache = new ArrayList<>();
    private volatile long lastCacheCheck = 0;
    private volatile long cachedDbCount = 0;

    // Danh sách Stopwords tiếng Việt phổ biến không dùng làm từ khóa tìm kiếm đơn độc
    private static final Set<String> STOPWORDS = Set.of(
            "vao", "trong", "cho", "duoc", "cac", "nhung", "co", "khi", "la", "va", "thi", "o", "cua", "em", "da", "nhe", "tai", "hay", "voi", "ve", "den", "nhu", "nen"
    );

    // Bản đồ từ đồng nghĩa thuật ngữ học vụ HCMUTE
    private static final Map<String, List<String>> ACADEMIC_SYNONYMS = Map.of(
            "anh van", List.of("tieng anh", "ngoai ngu", "avdv", "avdr", "toeic"),
            "tieng anh", List.of("anh van", "ngoai ngu", "avdv", "avdr", "toeic"),
            "dau vao", List.of("avdv", "dgnlta", "xet tuyen"),
            "dau ra", List.of("avdr", "chuan dau ra", "tot nghiep"),
            "avdv", List.of("anh van dau vao", "tieng anh dau vao", "ngoai ngu dau vao"),
            "avdr", List.of("anh van dau ra", "tieng anh dau ra", "toeic dau ra"),
            "dkmh", List.of("dang ky mon hoc", "dang ky tin chi", "tin chi"),
            "hoc phi", List.of("dong tien", "hoc phi tin chi", "bien lai"),
            "tot nghiep", List.of("xet tot nghiep", "bang tot nghiep", "le tot nghiep")
    );

    @PostConstruct
    public void initFaqKnowledgeBase() {
        reloadCache();
    }

    /**
     * Nạp toàn bộ FAQs từ Database vào RAM Cache
     */
    @Transactional(readOnly = true)
    public synchronized void reloadCache() {
        try {
            List<Faq> faqs = faqRepository.findAllActiveWithDepartment();
            faqCache.clear();
            for (Faq f : faqs) {
                String deptName = (f.getDepartment() != null) ? f.getDepartment().getName() : "Phòng Đào tạo";
                Long deptId = (f.getDepartment() != null) ? f.getDepartment().getId() : null;
                String category = (f.getCategory() != null) ? f.getCategory() : "Học vụ";
                String keywords = (f.getKeywords() != null) ? f.getKeywords() : "";
                LocalDateTime postDate = f.getPostDate() != null ? f.getPostDate() : f.getCreatedAt();
                String formattedDate = postDate != null ? postDate.format(DATE_FORMATTER) : "";

                faqCache.add(new FaqCacheItem(
                        f.getId(),
                        f.getQuestion(),
                        f.getAnswer(),
                        deptName,
                        deptId,
                        category,
                        normalize(f.getQuestion()),
                        normalize(keywords),
                        normalize(f.getAnswer()),
                        postDate,
                        formattedDate
                ));
            }
            cachedDbCount = faqCache.size();
            lastCacheCheck = System.currentTimeMillis();
            log.info("Nạp bộ nhớ Cache FAQ thành công: {} câu hỏi học vụ", faqCache.size());
        } catch (Exception e) {
            log.error("Lỗi khi nạp bộ nhớ Cache FAQ: ", e);
        }
    }

    /**
     * Tự động kiểm tra và làm mới Cache nếu số lượng câu hỏi trong Database thay đổi
     */
    private void checkAndSyncCache() {
        long now = System.currentTimeMillis();
        if (now - lastCacheCheck > 10_000) {
            lastCacheCheck = now;
            try {
                long currentCount = faqRepository.countActive();
                if (currentCount != cachedDbCount) {
                    log.info("Phát hiện thay đổi số lượng FAQ (DB: {}, Cache: {}). Tự động đồng bộ lại RAM Cache...", currentCount, cachedDbCount);
                    reloadCache();
                }
            } catch (Exception e) {
                log.debug("Bỏ qua lỗi kiểm tra count FAQ: {}", e.getMessage());
            }
        }
    }

    /**
     * Tìm kiếm thông minh câu hỏi học vụ theo từ khóa, viết tắt và từ đồng nghĩa học thuật
     */
    public List<FaqMatchResult> matchQuestion(String query, Long departmentId) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        checkAndSyncCache();

        // 1. Tự động mở rộng viết tắt học vụ HCMUTE qua AcademicAbbreviationUtils
        String expandedQuery = AcademicAbbreviationUtils.expand(query);
        String normQuery = normalize(expandedQuery);
        String[] rawTokens = normQuery.split("\\s+");
        List<String> queryWords = new ArrayList<>();
        List<String> meaningfulKeywords = new ArrayList<>();

        for (String w : rawTokens) {
            String trimmed = w.trim();
            if (!trimmed.isEmpty() && trimmed.length() >= 2) {
                queryWords.add(trimmed);
                if (!STOPWORDS.contains(trimmed)) {
                    meaningfulKeywords.add(trimmed);
                }
            }
        }
        if (meaningfulKeywords.isEmpty() && !queryWords.isEmpty()) {
            meaningfulKeywords.addAll(queryWords);
        }

        // Thu thập các cụm từ đồng nghĩa có thể xuất hiện từ câu truy vấn
        List<String> synonymPhrases = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : ACADEMIC_SYNONYMS.entrySet()) {
            if (normQuery.contains(entry.getKey())) {
                synonymPhrases.addAll(entry.getValue());
            }
        }

        // Thu thập các cụm từ ghép (Bigrams) từ câu truy vấn (Ví dụ: "anh van", "dau vao")
        List<String> queryBigrams = new ArrayList<>();
        for (int i = 0; i < rawTokens.length - 1; i++) {
            String b = rawTokens[i].trim() + " " + rawTokens[i + 1].trim();
            if (b.length() >= 5) {
                queryBigrams.add(b);
            }
        }

        List<ScoredItem> matches = new ArrayList<>();

        for (FaqCacheItem item : faqCache) {
            if (departmentId != null && !Objects.equals(item.departmentId(), departmentId)) {
                continue;
            }

            String paddedQuestion = " " + item.normQuestion() + " ";
            String paddedKeywords = " " + item.normKeywords() + " ";
            String paddedAnswer = " " + item.normAnswer() + " ";

            int relevanceScore = 0;
            int matchedQuestionWords = 0;
            int matchedAnswerWords = 0;
            boolean hasPhraseOrBigramMatch = false;

            // 1. Khớp trọn vẹn cụm từ người dùng tìm kiếm (Full Phrase Match)
            if (paddedQuestion.contains(" " + normQuery + " ")) {
                relevanceScore += 160;
                matchedQuestionWords += meaningfulKeywords.size();
                hasPhraseOrBigramMatch = true;
            } else if (item.normQuestion().contains(normQuery)) {
                relevanceScore += 120;
                matchedQuestionWords += meaningfulKeywords.size();
                hasPhraseOrBigramMatch = true;
            } else if (paddedAnswer.contains(" " + normQuery + " ")) {
                relevanceScore += 45;
                matchedAnswerWords += meaningfulKeywords.size();
                hasPhraseOrBigramMatch = true;
            }

            // 2. Khớp cụm từ đồng nghĩa học vụ
            for (String syn : synonymPhrases) {
                if (paddedQuestion.contains(" " + syn + " ") || item.normQuestion().contains(syn)) {
                    relevanceScore += 90;
                    matchedQuestionWords++;
                    hasPhraseOrBigramMatch = true;
                } else if (paddedAnswer.contains(" " + syn + " ")) {
                    relevanceScore += 30;
                    matchedAnswerWords++;
                }
            }

            // 3. Khớp cụm từ ghép (Bigrams) như "anh van", "dau vao", "tin chi"
            for (String bg : queryBigrams) {
                if (paddedQuestion.contains(" " + bg + " ") || item.normQuestion().contains(bg)) {
                    relevanceScore += 50;
                    matchedQuestionWords += 2;
                    hasPhraseOrBigramMatch = true;
                } else if (paddedAnswer.contains(" " + bg + " ")) {
                    relevanceScore += 15;
                    matchedAnswerWords += 2;
                }
            }

            // 4. Khớp từng từ khóa có nghĩa theo ranh giới từ nguyên vẹn
            for (String kw : meaningfulKeywords) {
                String paddedKw = " " + kw + " ";
                boolean inQ = paddedQuestion.contains(paddedKw);
                boolean inK = paddedKeywords.contains(paddedKw);
                boolean inA = paddedAnswer.contains(paddedKw);

                if (inQ) {
                    matchedQuestionWords++;
                    relevanceScore += 20;
                } else if (inK) {
                    matchedQuestionWords++;
                    relevanceScore += 15;
                } else if (inA) {
                    matchedAnswerWords++;
                    relevanceScore += 5;
                }
            }

            // Bộ lọc chặn rác: Với truy vấn đa từ, bắt buộc khớp phrase/bigram hoặc ít nhất 2 từ khóa
            if (meaningfulKeywords.size() >= 2) {
                if (!hasPhraseOrBigramMatch && matchedQuestionWords < 2 && (matchedQuestionWords + matchedAnswerWords < 3)) {
                    continue;
                }
            } else {
                if (matchedQuestionWords == 0 && matchedAnswerWords < 2) {
                    continue;
                }
            }

            // Điểm thưởng khi câu hỏi phủ kín đa số từ khóa tìm kiếm
            if (!meaningfulKeywords.isEmpty() && matchedQuestionWords >= meaningfulKeywords.size()) {
                relevanceScore += 40;
            }

            if (relevanceScore > 0) {
                double confidenceScore = Math.min(1.0, relevanceScore / 100.0);
                matches.add(new ScoredItem(item, relevanceScore, confidenceScore));
            }
        }

        // Sắp xếp: Ưu tiên điểm phù hợp (score DESC), rồi đến ngày cập nhật (postDate DESC)
        Comparator<ScoredItem> sorter = Comparator
                .comparingInt(ScoredItem::score).reversed()
                .thenComparing((ScoredItem s) -> s.item().postDate(), Comparator.nullsLast(Comparator.reverseOrder()));

        return matches.stream()
                .sorted(sorter)
                .limit(25)
                .map(m -> new FaqMatchResult(
                        m.item().id(),
                        m.item().question(),
                        m.item().answer(),
                        m.item().departmentName(),
                        m.item().departmentId(),
                        m.item().category(),
                        m.confidenceScore(),
                        m.item().postDate(),
                        m.item().formattedDate()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Chuẩn hóa chuỗi tiếng Việt: Bỏ dấu, chuyển chữ thường, xóa ký tự đặc biệt
     */
    public String normalize(String input) {
        if (input == null) return "";
        String temp = Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccents = pattern.matcher(temp).replaceAll("").replace("đ", "d");
        return noAccents.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    public record FaqMatchResult(
            Long id,
            String question,
            String answer,
            String departmentName,
            Long departmentId,
            String category,
            double confidenceScore,
            LocalDateTime postDate,
            String formattedDate
    ) {}

    private record FaqCacheItem(
            Long id,
            String question,
            String answer,
            String departmentName,
            Long departmentId,
            String category,
            String normQuestion,
            String normKeywords,
            String normAnswer,
            LocalDateTime postDate,
            String formattedDate
    ) {}

    private record ScoredItem(
            FaqCacheItem item,
            int score,
            double confidenceScore
    ) {}
}

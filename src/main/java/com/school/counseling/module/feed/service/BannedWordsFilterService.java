package com.school.counseling.module.feed.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * BỘ LỌC TỪ NGỮ VI PHẠM & KIỂM DUYỆT TỰ ĐỘNG (PROFANITY & BANNED WORDS FILTER)
 * 
 * Kiểm duyệt tự động nội dung bài viết thảo luận và bình luận trên Diễn đàn sinh viên:
 * 1. Phát hiện từ ngữ thô tục, xúc phạm danh dự, kích động bạo lực, cờ bạc, spam.
 * 2. Cung cấp hàm làm mờ (Masking - thay thế bằng dấu ***).
 * 3. Cung cấp hàm chặn (Validation - ném ngoại lệ từ chối đăng bài nếu vi phạm nghiêm trọng).
 */
@Slf4j
@Service
public class BannedWordsFilterService {

    private static final Set<String> BANNED_WORDS = new HashSet<>(Arrays.asList(
            // Từ ngữ thô tục, chửi thề
            "đm", "đmm", "dmm", "vcl", "vkl", "clgt", "đệt", "đụ", "đĩ", "chó đẻ",
            "khốn nạn", "thằng chó", "đồ ngu", "óc chó", "bố mày", "mẹ mày",
            // Cờ bạc, cá độ, lừa đảo
            "cá độ", "đánh bạc", "lô đề", "kubet", "thabet", "tài xỉu", "game bài đổi thưởng",
            // Spam quảng cáo độc hại
            "mua bằng lái", "làm bằng đại học", "hack điểm", "mua điểm thi"
    ));

    /**
     * Kiểm tra chuỗi văn bản có chứa từ cấm hay không
     */
    public boolean containsBannedWords(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String normalized = content.toLowerCase();
        return BANNED_WORDS.stream().anyMatch(word -> containsWord(normalized, word));
    }

    /**
     * Tìm danh sách các từ cấm có trong chuỗi
     */
    public List<String> findBannedWords(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }
        String normalized = content.toLowerCase();
        return BANNED_WORDS.stream()
                .filter(word -> containsWord(normalized, word))
                .collect(Collectors.toList());
    }

    /**
     * Thay thế các từ cấm bằng dấu sao (Masking)
     * Ví dụ: "thằng khốn nạn" -> "thằng ***"
     */
    public String maskBannedWords(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String result = content;
        for (String word : BANNED_WORDS) {
            Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b");
            result = pattern.matcher(result).replaceAll("***");
        }
        return result;
    }

    /**
     * Kiểm tra nghiêm ngặt: ném ngoại lệ nếu phát hiện bài viết chứa từ ngữ vi phạm
     */
    public void validateContent(String content) {
        List<String> detected = findBannedWords(content);
        if (!detected.isEmpty()) {
            log.warn("Phát hiện từ ngữ vi phạm quy chuẩn cộng đồng: {}", detected);
            throw new IllegalArgumentException("Nội dung chứa từ ngữ không phù hợp với môi trường học đường ("
                    + String.join(", ", detected) + "). Vui lòng điều chỉnh lại trước khi đăng.");
        }
    }

    private boolean containsWord(String source, String word) {
        return source.contains(word);
    }
}

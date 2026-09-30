package com.school.counseling.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tiện ích chuyển đổi chuỗi văn bản tiếng Việt sang dạng URL Slug thân thiện SEO.
 */
public final class SlugUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s+]");
    private static final Pattern MULTI_DASH = Pattern.compile("-+");

    private SlugUtils() {
        // Utility class
    }

    /**
     * Chuyển chuỗi tiếng Việt có dấu thành URL slug không dấu.
     * <p>
     * Ví dụ: "Hướng dẫn Đăng ký Học phần 2026!" -> "huong-dan-dang-ky-hoc-phan-2026"
     *
     * @param input Chuỗi văn bản gốc
     * @return Chuỗi URL slug
     */
    public static String toSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        // 1. Chuẩn hóa ký tự đặc thù tiếng Việt
        String text = input.trim();
        text = text.replace("đ", "d").replace("Đ", "d");

        // 2. Tách dấu tiếng Việt (Normalizer NFD) và loại bỏ ký tự dấu tổ hợp
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noDiacritics = pattern.matcher(normalized).replaceAll("");

        // 3. Thay khoảng trắng thành dấu gạch nối
        String nowhitespace = WHITESPACE.matcher(noDiacritics).replaceAll("-");

        // 4. Loại bỏ toàn bộ ký tự không phải chữ/số/dấu gạch nối
        String normalizedSlug = NONLATIN.matcher(nowhitespace).replaceAll("");

        // 5. Rút gọn nhiều dấu gạch nối liên tiếp thành 1 dấu gạch nối
        normalizedSlug = MULTI_DASH.matcher(normalizedSlug).replaceAll("-");

        // 6. Xóa dấu gạch nối ở đầu và cuối chuỗi, chuyển thành chữ thường
        if (normalizedSlug.startsWith("-")) {
            normalizedSlug = normalizedSlug.substring(1);
        }
        if (normalizedSlug.endsWith("-")) {
            normalizedSlug = normalizedSlug.substring(0, normalizedSlug.length() - 1);
        }

        return normalizedSlug.toLowerCase(Locale.ENGLISH);
    }
}

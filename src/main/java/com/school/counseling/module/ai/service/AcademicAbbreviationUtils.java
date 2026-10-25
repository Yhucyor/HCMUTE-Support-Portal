package com.school.counseling.module.ai.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiện ích chuẩn hóa teencode và từ viết tắt học vụ HCMUTE trước khi tra cứu ngữ nghĩa.
 * Đảm bảo câu hỏi viết tắt của sinh viên không bị tụt điểm tương đồng.
 */
public final class AcademicAbbreviationUtils {

    private static final Map<Pattern, String> ABBREVIATION_MAP = new LinkedHashMap<>();

    static {
        addRule("avđv|avdv", "anh văn đầu vào");
        addRule("avđr|avdr", "anh văn đầu ra");
        addRule("đrl|drl", "điểm rèn luyện");
        addRule("đkmh|dkmh|đkhp|dkhp", "đăng ký học phần");
        addRule("rút môn học|rut mon hoc|rút môn|rut mon|rút hp|rut hp", "rút học phần");
        addRule("hủy môn|huy mon|hủy môn học|huy mon hoc", "hủy học phần");
        addRule("hoãn thi|hoan thi|thi bù|thi bu|thi bổ sung|thi bo sung", "hoãn thi thi bổ sung");
        addRule("phúc khảo|phuc khao|pk", "phúc khảo điểm thi");
        addRule("học phí|hoc phi|tiền học|tien hoc", "học phí học phần");
        addRule("bảo lưu|bao luu|nghỉ học tạm thời|nghi hoc tam thoi", "bảo lưu tạm dừng học kỳ");
        addRule("hb", "học bổng");
        addRule("tn", "tốt nghiệp");
        addRule("kltn", "khóa luận tốt nghiệp");
        addRule("đatn", "đồ án tốt nghiệp");
        addRule("ctsv", "công tác sinh viên");
        addRule("pđt|pdt", "phòng đào tạo");
        addRule("hp", "học phần môn học");
        addRule("tc", "tín chỉ");
        addRule("gpa", "điểm trung bình tích lũy");
        addRule("nam hc|nam hoc", "năm học");
        addRule("shdk|shđk|sinh hoat dau nam|sinh hoạt đầu năm", "sinh hoạt đầu khóa tân sinh viên");
        addRule("khoa it|khoa cntt", "khoa công nghệ thông tin");
        addRule("it", "công nghệ thông tin");
        addRule("cntt", "công nghệ thông tin");
        addRule("gv|cbgd", "giảng viên");
        addRule("sv", "sinh viên");
        addRule("ccta|cc ta", "chứng chỉ tiếng anh");
        addRule("đgnlta|dgnlta", "đánh giá năng lực tiếng anh");
        addRule("dot 1 va 2|đợt 1 và 2", "đợt 1 và đợt 2");
    }

    private static void addRule(String regex, String replacement) {
        Pattern pattern = Pattern.compile("(?i)(?<!\\p{L})(" + regex + ")(?!\\p{L})", Pattern.UNICODE_CHARACTER_CLASS);
        ABBREVIATION_MAP.put(pattern, replacement);
    }

    private AcademicAbbreviationUtils() {}

    /**
     * Mở rộng các từ viết tắt trong câu hỏi sinh viên
     *
     * @param text Câu hỏi thô của sinh viên
     * @return Câu hỏi đã được mở rộng từ viết tắt
     */
    public static String expand(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text;
        for (Map.Entry<Pattern, String> entry : ABBREVIATION_MAP.entrySet()) {
            Matcher matcher = entry.getKey().matcher(result);
            result = matcher.replaceAll(entry.getValue());
        }
        return result;
    }
}

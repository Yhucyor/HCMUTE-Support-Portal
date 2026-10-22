package com.school.counseling.module.academic.service;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Bộ công cụ quy đổi điểm và xử lý nghiệp vụ đánh giá học vụ chuẩn HCMUTE (2025).
 * Căn cứ:
 * - Quy chế đào tạo ĐH: Quyết định số 3116/QĐ-ĐHSPKT (22/8/2025)
 * - Quy định học bổng: Quyết định số 409/QĐ-ĐHSPKT (05/02/2024)
 * - Sổ tay sinh viên HCMUTE 2025
 */
@Component
public class HcmuteGradingEngine {

    @Getter
    @Builder
    @ToString
    public static class GradeConversion {
        private String letter;
        private Double gpa4;
        private boolean isPassed;
        private boolean isCountedInGpa;
        private boolean isCountedInAccumulatedCredits;
    }

    @Getter
    @Builder
    @ToString
    public static class AcademicWarningResult {
        private boolean isWarning;
        private String warningReason;
        private int totalDebtCredits;
    }

    /**
     * Làm tròn 1 chữ số thập phân chuẩn quy chế đào tạo HCMUTE
     */
    public static double round1Decimal(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Làm tròn 2 chữ số thập phân cho GPA
     */
    public static double round2Decimals(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Kiểm tra học kỳ có thuộc diện quy chế lịch sử (HK1/2021-2022 đến HK1/2022-2023 điểm D vẫn đạt)
     */
    public static boolean isLegacyPassSemester(String semester) {
        if (semester == null || semester.isBlank()) return false;
        String s = semester.toLowerCase(Locale.ROOT);
        boolean y2122 = s.contains("2021-2022") || s.contains("2021 - 2022");
        boolean y2223Hk1 = (s.contains("2022-2023") || s.contains("2022 - 2023"))
                && (s.contains("hk01") || s.contains("học kỳ 1") || s.contains("hoc ky 1") || s.contains("hk1"));
        return y2122 || y2223Hk1;
    }

    /**
     * Nhận diện học phần điều kiện (GDTC, GDQP-AN)
     * Các môn này bắt buộc để tốt nghiệp nhưng KHÔNG tính vào GPA và không tính vào tín chỉ tích lũy chuyên môn.
     */
    public static boolean isConditionalSubject(String courseCode, String courseName) {
        if (courseCode != null) {
            String codeUpper = courseCode.trim().toUpperCase(Locale.ROOT);
            if (codeUpper.startsWith("PHED") || codeUpper.startsWith("MILT")
                    || codeUpper.startsWith("GDTC") || codeUpper.startsWith("GDQP")
                    || codeUpper.startsWith("LLCT") && codeUpper.contains("QP")) {
                return true;
            }
        }
        if (courseName != null) {
            String nameLower = courseName.trim().toLowerCase(Locale.ROOT);
            if (nameLower.contains("thể chất") || nameLower.contains("thể dục")
                    || nameLower.contains("quốc phòng") || nameLower.contains("an ninh")
                    || nameLower.contains("bóng đá") || nameLower.contains("bóng chuyền")
                    || nameLower.contains("bóng bàn") || nameLower.contains("bơi lội")
                    || nameLower.contains("cầu lông") || nameLower.contains("võ ")
                    || nameLower.contains("điền kinh")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Nhận diện môn Kỹ năng giao tiếp tiếng Anh 1, 2 (áp dụng SV từ K23 không tính xét học bổng)
     */
    public static boolean isEnglishCommunication12(String courseCode, String courseName) {
        if (courseCode != null) {
            String c = courseCode.toUpperCase(Locale.ROOT);
            if (c.contains("ENCO") || c.contains("ECOM") || c.contains("ENGL11") || c.contains("ENGL12")) {
                return true;
            }
        }
        if (courseName != null) {
            String n = courseName.toLowerCase(Locale.ROOT);
            return n.contains("kỹ năng giao tiếp tiếng anh 1") || n.contains("kỹ năng giao tiếp tiếng anh 2")
                    || n.contains("giao tiếp tiếng anh 1") || n.contains("giao tiếp tiếng anh 2");
        }
        return false;
    }

    /**
     * Quy đổi điểm Thang 10 sang Điểm chữ, Điểm thang 4 và Trạng thái Đạt/Không đạt chuẩn HCMUTE 2025.
     * Quy tắc:
     * - A+ (9.0 - 10.0) -> 4.0, Đạt
     * - A  (8.5 - 8.9)  -> 3.7, Đạt
     * - B+ (8.0 - 8.4)  -> 3.5, Đạt
     * - B  (7.0 - 7.9)  -> 3.0, Đạt
     * - C+ (6.5 - 6.9)  -> 2.5, Đạt
     * - C  (5.5 - 6.4)  -> 2.0, Đạt
     * - D+ (5.0 - 5.4)  -> 1.5, Đạt
     * - D  (4.0 - 4.9)  -> 1.0, KHÔNG ĐẠT (Trừ ngoại lệ lịch sử HK1/21-22 -> HK1/22-23)
     * - F  (< 4.0)      -> 0.0, KHÔNG ĐẠT
     *
     * Điểm chữ đặc biệt:
     * - P: Đạt điều kiện, không tính GPA
     * - R: Miễn học, cộng tín chỉ tích lũy, không tính GPA
     * - I, X: Hoãn thi / chưa hoàn thiện, không tính GPA, không cộng tín chỉ
     */
    public static GradeConversion convertGrade(Double score10, String letter, String semester,
                                              String courseCode, String courseName) {
        String cleanLetter = letter != null ? letter.trim().toUpperCase(Locale.ROOT) : "";
        boolean isLegacy = isLegacyPassSemester(semester);
        boolean isCond = isConditionalSubject(courseCode, courseName);

        // 1. Xử lý các điểm chữ đặc biệt
        if ("P".equals(cleanLetter)) {
            return GradeConversion.builder()
                    .letter("P")
                    .gpa4(null)
                    .isPassed(true)
                    .isCountedInGpa(false)
                    .isCountedInAccumulatedCredits(false) // GDTC, GDQP xét tốt nghiệp riêng
                    .build();
        }
        if ("R".equals(cleanLetter)) {
            return GradeConversion.builder()
                    .letter("R")
                    .gpa4(null)
                    .isPassed(true)
                    .isCountedInGpa(false)
                    .isCountedInAccumulatedCredits(true) // Miễn học được cộng tín chỉ tích lũy
                    .build();
        }
        if ("I".equals(cleanLetter) || "X".equals(cleanLetter)) {
            return GradeConversion.builder()
                    .letter(cleanLetter)
                    .gpa4(null)
                    .isPassed(false)
                    .isCountedInGpa(false)
                    .isCountedInAccumulatedCredits(false)
                    .build();
        }

        // Nếu là môn điều kiện nhưng có điểm số / điểm chữ thông thường -> Không tính vào GPA
        if (isCond) {
            boolean passed = ("D+".equals(cleanLetter) || cleanLetter.startsWith("A") || cleanLetter.startsWith("B") || cleanLetter.startsWith("C"))
                    || (score10 != null && score10 >= 5.0);
            return GradeConversion.builder()
                    .letter(!cleanLetter.isBlank() ? cleanLetter : (passed ? "P" : "F"))
                    .gpa4(null)
                    .isPassed(passed)
                    .isCountedInGpa(false)
                    .isCountedInAccumulatedCredits(false)
                    .build();
        }

        // 2. Nếu đã có điểm chữ chuẩn từ Portal
        if (!cleanLetter.isBlank() && !"--".equals(cleanLetter)) {
            return switch (cleanLetter) {
                case "A+" -> GradeConversion.builder().letter("A+").gpa4(4.0).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
                case "A"  -> GradeConversion.builder().letter("A").gpa4(3.7).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
                case "B+" -> GradeConversion.builder().letter("B+").gpa4(3.5).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
                case "B"  -> GradeConversion.builder().letter("B").gpa4(3.0).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
                case "C+" -> GradeConversion.builder().letter("C+").gpa4(2.5).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
                case "C"  -> GradeConversion.builder().letter("C").gpa4(2.0).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
                case "D+" -> GradeConversion.builder().letter("D+").gpa4(1.5).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
                case "D"  -> GradeConversion.builder().letter("D").gpa4(1.0).isPassed(isLegacy).isCountedInGpa(true).isCountedInAccumulatedCredits(isLegacy).build();
                case "F"  -> GradeConversion.builder().letter("F").gpa4(0.0).isPassed(false).isCountedInGpa(true).isCountedInAccumulatedCredits(false).build();
                default   -> convertFromScore10(score10, isLegacy);
            };
        }

        // 3. Quy đổi từ Điểm hệ 10 nếu điểm chữ chưa có
        return convertFromScore10(score10, isLegacy);
    }

    private static GradeConversion convertFromScore10(Double score10, boolean isLegacy) {
        if (score10 == null) {
            return GradeConversion.builder()
                    .letter("--")
                    .gpa4(null)
                    .isPassed(false)
                    .isCountedInGpa(false)
                    .isCountedInAccumulatedCredits(false)
                    .build();
        }

        double s = round1Decimal(score10);
        if (s >= 9.0) {
            return GradeConversion.builder().letter("A+").gpa4(4.0).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
        } else if (s >= 8.5) {
            return GradeConversion.builder().letter("A").gpa4(3.7).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
        } else if (s >= 8.0) {
            return GradeConversion.builder().letter("B+").gpa4(3.5).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
        } else if (s >= 7.0) {
            return GradeConversion.builder().letter("B").gpa4(3.0).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
        } else if (s >= 6.5) {
            return GradeConversion.builder().letter("C+").gpa4(2.5).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
        } else if (s >= 5.5) {
            return GradeConversion.builder().letter("C").gpa4(2.0).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
        } else if (s >= 5.0) {
            return GradeConversion.builder().letter("D+").gpa4(1.5).isPassed(true).isCountedInGpa(true).isCountedInAccumulatedCredits(true).build();
        } else if (s >= 4.0) {
            return GradeConversion.builder().letter("D").gpa4(1.0).isPassed(isLegacy).isCountedInGpa(true).isCountedInAccumulatedCredits(isLegacy).build();
        } else {
            return GradeConversion.builder().letter("F").gpa4(0.0).isPassed(false).isCountedInGpa(true).isCountedInAccumulatedCredits(false).build();
        }
    }

    /**
     * Bảng xếp loại học lực / Xếp hạng tốt nghiệp theo Thang điểm 4 (Mục 3.2)
     */
    public static String resolveAcademicStanding(Double gpa4) {
        if (gpa4 == null) return "Chưa có dữ liệu";
        if (gpa4 >= 3.6) return "Xuất sắc";
        if (gpa4 >= 3.2) return "Giỏi";
        if (gpa4 >= 2.5) return "Khá";
        if (gpa4 >= 2.0) return "Trung bình";
        if (gpa4 >= 1.0) return "Yếu";
        return "Kém";
    }

    /**
     * Xác định trình độ năm đào tạo dựa trên tổng số tín chỉ tích lũy (Mục 3.4)
     */
    public static String resolveAcademicYearLevel(int accumulatedCredits) {
        if (accumulatedCredits < 36) return "Năm thứ nhất";
        if (accumulatedCredits <= 71) return "Năm thứ hai";
        if (accumulatedCredits <= 107) return "Năm thứ ba";
        if (accumulatedCredits <= 149) return "Năm thứ tư";
        return "Năm thứ năm";
    }

    /**
     * Đánh giá Cảnh báo học tập theo Quy chế HCMUTE 2025 (Mục 4.2)
     * Tiêu chí:
     * 1. Tổng số tín chỉ nợ đọng > 24 tín chỉ
     * 2. Tỷ lệ TC không đạt trong học kỳ gần nhất > 50% tổng TC đăng ký
     * 3. Điểm trung bình tích lũy (ĐTBTL) thấp hơn ngưỡng theo năm đào tạo:
     *    - Năm 1 (< 36 TC): GPA < 1.20
     *    - Năm 2 (36 - 71 TC): GPA < 1.40
     *    - Năm 3 (72 - 107 TC): GPA < 1.60
     *    - Năm 4 trở đi (>= 108 TC): GPA < 1.80
     */
    public static AcademicWarningResult evaluateAcademicWarning(int totalDebtCredits,
                                                               Double latestSemesterFailRatio,
                                                               Double cumulativeGpa4,
                                                               int accumulatedCredits) {
        StringBuilder reasons = new StringBuilder();
        boolean isWarning = false;

        // Tiêu chí 1: Nợ đọng tín chỉ > 24 TC
        if (totalDebtCredits > 24) {
            isWarning = true;
            reasons.append("Tổng tín chỉ nợ đọng vượt quá 24 TC (hiện nợ ").append(totalDebtCredits).append(" TC). ");
        }

        // Tiêu chí 2: Rớt > 50% số TC đăng ký trong học kỳ
        if (latestSemesterFailRatio != null && latestSemesterFailRatio > 0.5) {
            isWarning = true;
            int pct = (int) Math.round(latestSemesterFailRatio * 100);
            reasons.append("Tỷ lệ tín chỉ không đạt trong học kỳ vượt quá 50% (đạt ").append(pct).append("%). ");
        }

        // Tiêu chí 3: ĐTBTL dưới ngưỡng năm đào tạo
        if (cumulativeGpa4 != null && cumulativeGpa4 > 0.0) {
            double threshold;
            String yearName;
            if (accumulatedCredits < 36) {
                threshold = 1.20;
                yearName = "Năm 1";
            } else if (accumulatedCredits <= 71) {
                threshold = 1.40;
                yearName = "Năm 2";
            } else if (accumulatedCredits <= 107) {
                threshold = 1.60;
                yearName = "Năm 3";
            } else {
                threshold = 1.80;
                yearName = "Năm 4 trở lên";
            }

            if (cumulativeGpa4 < threshold) {
                isWarning = true;
                reasons.append(String.format(Locale.ROOT, "ĐTB tích lũy (%.2f) thấp hơn ngưỡng quy định %s (< %.2f). ",
                        cumulativeGpa4, yearName, threshold));
            }
        }

        return AcademicWarningResult.builder()
                .isWarning(isWarning)
                .warningReason(isWarning ? reasons.toString().trim() : "Bình thường - Không vi phạm quy chế cảnh báo")
                .totalDebtCredits(totalDebtCredits)
                .build();
    }
}

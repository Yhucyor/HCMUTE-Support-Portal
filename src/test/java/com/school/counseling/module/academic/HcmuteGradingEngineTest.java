package com.school.counseling.module.academic;

import com.school.counseling.module.academic.service.HcmuteGradingEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UNIT TEST - MODULE ACADEMIC: Quy đổi điểm & Đánh giá học vụ HCMUTE (2025)")
class HcmuteGradingEngineTest {

    @Test
    @DisplayName("Quy đổi điểm chữ chuẩn thang điểm 4")
    void testStandardLetterConversions() {
        var aPlus = HcmuteGradingEngine.convertGrade(null, "A+", "HK01 (2024-2025)", "MATH1301", "Toán 1");
        assertEquals(4.0, aPlus.getGpa4());
        assertTrue(aPlus.isPassed());
        assertTrue(aPlus.isCountedInGpa());

        var b = HcmuteGradingEngine.convertGrade(null, "B", "HK01 (2024-2025)", "MATH1301", "Toán 1");
        assertEquals(3.0, b.getGpa4());
        assertTrue(b.isPassed());

        var f = HcmuteGradingEngine.convertGrade(null, "F", "HK01 (2024-2025)", "MATH1301", "Toán 1");
        assertEquals(0.0, f.getGpa4());
        assertFalse(f.isPassed());
    }

    @Test
    @DisplayName("Ngoại lệ lịch sử: Điểm D trong HK1 2021-2022 tính Đạt, nhưng 2024-2025 tính Không đạt")
    void testLegacyPassSemesterForGradeD() {
        // HK1 2021-2022: Điểm D được tính Đạt theo quy chế đặc thù Covid
        var dLegacy = HcmuteGradingEngine.convertGrade(null, "D", "HK01 (2021-2022)", "COMP1401", "Nhập môn LT");
        assertEquals(1.0, dLegacy.getGpa4());
        assertTrue(dLegacy.isPassed());

        // HK01 2024-2025: Điểm D không đạt chuẩn đầu ra môn học
        var dCurrent = HcmuteGradingEngine.convertGrade(null, "D", "HK01 (2024-2025)", "COMP1401", "Nhập môn LT");
        assertEquals(1.0, dCurrent.getGpa4());
        assertFalse(dCurrent.isPassed());
    }

    @Test
    @DisplayName("Môn điều kiện (GDTC, GDQP): Không tính vào GPA và không cộng vào tín chỉ tích lũy chuyên môn")
    void testConditionalSubjects() {
        var gdtc = HcmuteGradingEngine.convertGrade(8.5, "A", "HK01 (2024-2025)", "PHED1101", "Giáo dục thể chất 1");
        assertTrue(gdtc.isPassed());
        assertFalse(gdtc.isCountedInGpa());
        assertFalse(gdtc.isCountedInAccumulatedCredits());

        var gdqp = HcmuteGradingEngine.convertGrade(9.0, "A+", "HK01 (2024-2025)", "MILT1201", "Giáo dục quốc phòng");
        assertTrue(gdqp.isPassed());
        assertFalse(gdqp.isCountedInGpa());
    }

    @Test
    @DisplayName("Cảnh báo học tập: Phát hiện nợ đọng > 24 tín chỉ hoặc GPA dưới ngưỡng")
    void testAcademicWarningEvaluation() {
        // TH1: Nợ đọng 26 tín chỉ (> 24 TC) -> Bị cảnh báo
        var warningDebt = HcmuteGradingEngine.evaluateAcademicWarning(26, 0.1, 2.5, 50);
        assertTrue(warningDebt.isWarning());
        assertTrue(warningDebt.getWarningReason().contains("24 TC"));

        // TH2: Năm 1 có GPA tích lũy 1.10 (< 1.20) -> Bị cảnh báo
        var warningGpa = HcmuteGradingEngine.evaluateAcademicWarning(10, 0.2, 1.10, 20);
        assertTrue(warningGpa.isWarning());
        assertTrue(warningGpa.getWarningReason().contains("1.20"));

        // TH3: Sinh viên học tập bình thường -> Không cảnh báo
        var normal = HcmuteGradingEngine.evaluateAcademicWarning(4, 0.0, 3.2, 45);
        assertFalse(normal.isWarning());
    }

    @Test
    @DisplayName("Xếp loại học lực theo GPA thang điểm 4")
    void testAcademicStanding() {
        assertEquals("Xuất sắc", HcmuteGradingEngine.resolveAcademicStanding(3.8));
        assertEquals("Giỏi", HcmuteGradingEngine.resolveAcademicStanding(3.3));
        assertEquals("Khá", HcmuteGradingEngine.resolveAcademicStanding(2.8));
        assertEquals("Trung bình", HcmuteGradingEngine.resolveAcademicStanding(2.1));
        assertEquals("Yếu", HcmuteGradingEngine.resolveAcademicStanding(1.5));
    }
}

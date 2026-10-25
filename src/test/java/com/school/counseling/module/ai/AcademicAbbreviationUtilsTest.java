package com.school.counseling.module.ai;

import com.school.counseling.module.ai.service.AcademicAbbreviationUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UNIT TEST - MODULE AI: Chuẩn hóa teencode & từ viết tắt học vụ HCMUTE")
class AcademicAbbreviationUtilsTest {

    @Test
    @DisplayName("Mở rộng chuẩn xác các từ viết tắt học vụ phổ biến")
    void testExpandAbbreviations() {
        String input1 = "Khi nào có điểm drl kỳ này vậy thầy?";
        String output1 = AcademicAbbreviationUtils.expand(input1);
        assertTrue(output1.toLowerCase().contains("điểm rèn luyện"));

        String input2 = "Hạn chót dkhp và rút hp đợt 1";
        String output2 = AcademicAbbreviationUtils.expand(input2);
        assertTrue(output2.toLowerCase().contains("đăng ký học phần"));
        assertTrue(output2.toLowerCase().contains("học phần"));

        String input3 = "Phòng ctsv hay pdt phụ trách cấp giấy xác nhận sv?";
        String output3 = AcademicAbbreviationUtils.expand(input3);
        assertTrue(output3.toLowerCase().contains("công tác sinh viên"));
        assertTrue(output3.toLowerCase().contains("phòng đào tạo"));
        assertTrue(output3.toLowerCase().contains("sinh viên"));
    }

    @Test
    @DisplayName("Bảo toàn văn bản khi không có từ viết tắt hoặc input rỗng")
    void testExpand_NoAbbreviationOrNull() {
        assertNull(AcademicAbbreviationUtils.expand(null));
        assertEquals("", AcademicAbbreviationUtils.expand(""));
        assertEquals("   ", AcademicAbbreviationUtils.expand("   "));

        String normalText = "Lịch thi học kỳ chính thức của trường";
        assertEquals(normalText, AcademicAbbreviationUtils.expand(normalText));
    }

    @Test
    @DisplayName("Từ viết tắt dkmh, dkhp mở rộng chuẩn xác thành đăng ký học phần")
    void testCourseRegistrationSynonyms() {
        String input1 = "Khi nào bắt đầu đkmh học kỳ 2?";
        String output1 = AcademicAbbreviationUtils.expand(input1);
        assertTrue(output1.toLowerCase().contains("đăng ký học phần"));

        String input2 = "Hạn chót dkhp và rút hp đợt 1";
        String output2 = AcademicAbbreviationUtils.expand(input2);
        assertTrue(output2.toLowerCase().contains("đăng ký học phần"));
        assertTrue(output2.toLowerCase().contains("rút học phần"));
    }
}

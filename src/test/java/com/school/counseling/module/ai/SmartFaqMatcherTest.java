package com.school.counseling.module.ai;

import com.school.counseling.module.ai.entity.Faq;
import com.school.counseling.module.ai.repository.FaqRepository;
import com.school.counseling.module.ai.service.AcademicAbbreviationUtils;
import com.school.counseling.module.ai.service.SmartFaqMatcherService;
import com.school.counseling.module.auth.entity.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * UNIT TEST TDD: KIỂM THỬ THUẬT TOÁN ĐỐI SOÁT NGỮ NGHĨA FAQ & MỞ RỘNG TỪ VIẾT TẮT
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TDD-FAQ-01: Kiểm thử Semantic FAQ Matcher & Academic Abbreviations")
class SmartFaqMatcherTest {

    @Mock
    private FaqRepository faqRepository;

    @InjectMocks
    private SmartFaqMatcherService faqMatcherService;

    private Department deptDaoTao;
    private Department deptNn;

    @BeforeEach
    void setUp() {
        deptDaoTao = Department.builder().id(1L).name("Phòng Đào tạo").code("DAO_TAO").build();
        deptNn = Department.builder().id(2L).name("Khoa Ngoại ngữ").code("KHOA_NN").build();

        List<Faq> sampleFaqs = List.of(
                Faq.builder()
                        .id(1L)
                        .question("Quy định chuẩn đầu ra tiếng Anh TOEIC đối với sinh viên đại học chính quy?")
                        .answer("Sinh viên đại học chính quy phải đạt chuẩn đầu ra tiếng Anh tương đương TOEIC 500 điểm trước khi xét tốt nghiệp.")
                        .department(deptNn)
                        .category("Ngoại ngữ")
                        .keywords("tiếng anh, toeic, chuẩn đầu ra, avdr, tốt nghiệp")
                        .viewCount(150)
                        .postDate(LocalDateTime.now())
                        .isActive(true)
                        .build(),
                Faq.builder()
                        .id(2L)
                        .question("Thời gian và cách thức đóng học phí học kỳ 1 năm học 2026-2027?")
                        .answer("Học sinh sinh viên đóng học phí trực tuyến qua cổng thanh toán VNPAY hoặc chuyển khoản ngân hàng Agribank/BIDV theo mã số sinh viên.")
                        .department(deptDaoTao)
                        .category("Học phí")
                        .keywords("học phí, đóng tiền, vnpay, ngân hàng, học kỳ 1")
                        .viewCount(230)
                        .postDate(LocalDateTime.now())
                        .isActive(true)
                        .build(),
                Faq.builder()
                        .id(3L)
                        .question("Quy trình đăng ký môn học và đăng ký tín chỉ bổ sung đợt 2?")
                        .answer("Sinh viên đăng nhập hệ thống dkmh.hcmute.edu.vn vào khung giờ quy định để đăng ký môn học bổ sung.")
                        .department(deptDaoTao)
                        .category("Đăng ký môn học")
                        .keywords("đăng ký môn học, dkmh, tín chỉ, đợt 2")
                        .viewCount(310)
                        .postDate(LocalDateTime.now())
                        .isActive(true)
                        .build()
        );

        when(faqRepository.findAllActiveWithDepartment()).thenReturn(sampleFaqs);
        when(faqRepository.countActive()).thenReturn((long) sampleFaqs.size());

        faqMatcherService.reloadCache();
    }

    @Test
    @DisplayName("1. AcademicAbbreviationUtils: Tự động mở rộng từ viết tắt học vụ HCMUTE")
    void testAcademicAbbreviationExpansion() {
        assertEquals("anh văn đầu vào", AcademicAbbreviationUtils.expand("avđv"));
        assertEquals("anh văn đầu ra", AcademicAbbreviationUtils.expand("avđr"));
        assertEquals("đăng ký học phần", AcademicAbbreviationUtils.expand("dkmh"));
        assertEquals("điểm rèn luyện", AcademicAbbreviationUtils.expand("drl"));
        assertEquals("cho em hỏi về anh văn đầu ra", AcademicAbbreviationUtils.expand("cho em hỏi về avdr"));
    }

    @Test
    @DisplayName("2. SmartFaqMatcherService.normalize: Chuẩn hóa chuỗi tiếng Việt không dấu")
    void testNormalizeVietnamese() {
        String input = "Đại Học Sư Phạm Kỹ Thuật TP.HCM - Tuyển Sinh 2026!";
        String normalized = faqMatcherService.normalize(input);
        assertEquals("dai hoc su pham ky thuat tp hcm tuyen sinh 2026", normalized);
    }

    @Test
    @DisplayName("3. SmartFaqMatcherService.matchQuestion: Tìm kiếm chính xác câu hỏi học phí")
    void testMatchQuestion_TuitionFee() {
        List<SmartFaqMatcherService.FaqMatchResult> results = faqMatcherService.matchQuestion("đóng học phí học kỳ 1 như thế nào", null);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2L, results.get(0).id());
        assertTrue(results.get(0).question().contains("học phí"));
        assertTrue(results.get(0).confidenceScore() > 0.5);
    }

    @Test
    @DisplayName("4. SmartFaqMatcherService.matchQuestion: Tra cứu với từ viết tắt avdr")
    void testMatchQuestion_AbbreviationToeic() {
        List<SmartFaqMatcherService.FaqMatchResult> results = faqMatcherService.matchQuestion("chuẩn avdr toeic bao nhiêu", null);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1L, results.get(0).id());
        assertTrue(results.get(0).question().contains("tiếng Anh TOEIC"));
    }
}

package com.school.counseling.module.ai.dto;

/**
 * Record chứa thống kê số lượng câu hỏi theo từng danh mục thực tế.
 */
public record FaqCategoryCountDto(
        long totalCount,
        long countHocVu,
        long countHocPhi,
        long countQuyChe,
        long countNgoaiNgu,
        long countTaiKhoan,
        long countDichVu
) {}

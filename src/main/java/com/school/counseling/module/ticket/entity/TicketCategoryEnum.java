package com.school.counseling.module.ticket.entity;

import lombok.Getter;

/**
 * Danh mục loại thủ tục học vụ quy định mức độ ưu tiên và thời hạn cam kết SLA tự động
 * Hệ thống tự suy diễn Priority và SlaHours dựa trên loại thủ tục, không cho phép User tùy tiện chọn.
 */
@Getter
public enum TicketCategoryEnum {

    // ── NHÓM KHẨN CẤP (URGENT - SLA 24H) ─────────────────────────────────────
    DEFER_EXAM(
            "Đơn xin hoãn thi / Thi bổ sung",
            "URGENT",
            24,
            "DAO_TAO",
            "Dành cho sinh viên gặp sự cố bất khả kháng (sức khỏe, tang chế) xin thi bổ sung"
    ),
    ACADEMIC_SUSPENSION(
            "Tạm dừng tiến độ / Bảo lưu học kỳ",
            "URGENT",
            24,
            "DAO_TAO",
            "Đơn xin tạm dừng học tập, thực hiện nghĩa vụ quân sự hoặc điều trị bệnh"
    ),
    DROPOUT_REQUEST(
            "Đơn xin thôi học / Rút hồ sơ",
            "URGENT",
            24,
            "DAO_TAO",
            "Thủ tục thôi học theo nguyện vọng cá nhân hoặc chuyển trường"
    ),
    GRADUATION_APPEAL(
            "Cứu xét tốt nghiệp / Xét tốt nghiệp bổ sung",
            "URGENT",
            24,
            "DAO_TAO",
            "Hồ sơ cứu xét điều kiện xét tốt nghiệp đợt cuối, chuyển đổi tín chỉ tương đương"
    ),
    EXAM_GRADE_DISPUTE(
            "Khiếu nại sai sót điểm thi tốt nghiệp",
            "URGENT",
            24,
            "DAO_TAO",
            "Phúc khảo khẩn cấp điểm thi các học phần tốt nghiệp hoặc khóa luận"
    ),

    // ── NHÓM ƯU TIÊN CAO (HIGH - SLA 48H) ───────────────────────────────────
    LMS_PORTAL_LOCK(
            "Mở khóa tài khoản LMS / Cổng thi trắc nghiệm",
            "HIGH",
            48,
            "KHOA_CNTT",
            "Xử lý lỗi đăng nhập, khóa tài khoản học trực tuyến LMS hoặc hệ thống thi số"
    ),
    COURSE_REGISTRATION_ISSUE(
            "Lỗi đăng ký tín chỉ học phần tốt nghiệp",
            "HIGH",
            48,
            "DAO_TAO",
            "Hỗ trợ mở lớp, đăng ký môn học tiên quyết hoặc bổ sung nhóm học phần"
    ),
    SCHOLARSHIP_DISPUTE(
            "Thắc mắc / Khiếu nại học bổng khuyến khích",
            "HIGH",
            48,
            "DOAN_HOI",
            "Đối soát tiêu chí xét học bổng khuyến khích học tập và điểm rèn luyện"
    ),

    // ── NHÓM THÔNG THƯỜNG (MEDIUM - SLA 72H) ────────────────────────────────
    TUITION_RECONCILIATION(
            "Đối soát học phí ngân hàng / Trừ trùng",
            "MEDIUM",
            72,
            "DAO_TAO",
            "Xử lý giao dịch trừ tiền học phí nhưng chưa gạch nợ trên cổng đào tạo"
    ),
    TRANSCRIPT_REQUEST(
            "Trích lục bảng điểm / Xác nhận điểm rèn luyện",
            "MEDIUM",
            72,
            "DAO_TAO",
            "Cấp bảng điểm tạm thời, song ngữ Anh - Việt hoặc chứng nhận kết quả học tập"
    ),
    STUDENT_CARD_REISSUE(
            "Cấp lại thẻ sinh viên / Thẻ ngân hàng liên kết",
            "MEDIUM",
            72,
            "DOAN_HOI",
            "Thủ tục cấp lại thẻ SV bị mất hoặc hỏng chip điện tử"
    ),
    MILITARY_DEFERMENT(
            "Cấp giấy tạm hoãn nghĩa vụ quân sự",
            "MEDIUM",
            72,
            "DAO_TAO",
            "Xin giấy xác nhận sinh viên nộp cho Ban chỉ huy quân sự địa phương"
    ),

    // ── NHÓM TIÊU CHUẨN (LOW - SLA 120H) ────────────────────────────────────
    PROFILE_UPDATE(
            "Cập nhật thông tin CCCD / Hộ khẩu / Email",
            "LOW",
            120,
            "DAO_TAO",
            "Điều chỉnh số định danh cá nhân, quê quán, số điện thoại trong hồ sơ sinh viên"
    ),
    SURVEY_FEEDBACK(
            "Góp ý khảo sát chất lượng đào tạo",
            "LOW",
            120,
            "DAO_TAO",
            "Ý kiến phản hồi về cơ sở vật chất, chất lượng giảng dạy và dịch vụ hỗ trợ"
    ),
    GENERAL_INQUIRY(
            "Tư vấn học vụ / Câu hỏi chung",
            "LOW",
            120,
            "DAO_TAO",
            "Giải đáp thắc mắc quy chế đào tạo tín chỉ và lộ trình học tập"
    );

    private final String displayName;
    private final String priority;
    private final int slaHours;
    private final String defaultDepartmentCode;
    private final String description;

    TicketCategoryEnum(String displayName, String priority, int slaHours, String defaultDepartmentCode, String description) {
        this.displayName = displayName;
        this.priority = priority;
        this.slaHours = slaHours;
        this.defaultDepartmentCode = defaultDepartmentCode;
        this.description = description;
    }

    /**
     * Tìm kiếm enum từ mã chuỗi an toàn (Case-insensitive)
     */
    public static TicketCategoryEnum fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return GENERAL_INQUIRY;
        }
        for (TicketCategoryEnum cat : values()) {
            if (cat.name().equalsIgnoreCase(code.trim()) || cat.displayName.equalsIgnoreCase(code.trim())) {
                return cat;
            }
        }
        return GENERAL_INQUIRY;
    }
}

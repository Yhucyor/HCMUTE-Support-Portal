package com.school.counseling.module.ai.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.ai.dto.FaqCategoryCountDto;
import com.school.counseling.module.ai.dto.FaqItemDto;
import com.school.counseling.module.ai.service.FaqService;
import com.school.counseling.module.auth.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller điều hướng giao diện tra cứu FAQ.
 * Tuân thủ SoC: Chỉ phụ trách tiếp nhận request, gọi FaqService và truyền DTO vào Model.
 */
@Controller
@RequiredArgsConstructor
public class FaqWebController {

    private final FaqService faqService;

    @GetMapping({"/faqs", "/faq"})
    public String searchFaqs(
            @RequestParam(value = "q", required = false, defaultValue = "") String query,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            Model model) {

        // Luôn nạp trọn vẹn 351 câu hỏi ra View để Client-side Filter và Pagination
        // hoạt động mượt mà, không bao giờ bị cắt cụt còn 25 câu khi có param tìm kiếm
        List<FaqItemDto> allFaqs = faqService.getAllActiveFaqs();
        FaqCategoryCountDto counts = faqService.calculateCategoryCounts(allFaqs);

        SecurityUtils.getCurrentUser().ifPresent(currentUser -> {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("currentUserFullName", currentUser.getFullName());
            model.addAttribute("currentUserAvatar", currentUser.getAvatarUrl());
            String role = (currentUser.getRole() != null) ? currentUser.getRole().replace("ROLE_", "") : "STUDENT";
            model.addAttribute("currentUserRole", role);
        });

        model.addAttribute("isSearchMode", query != null && !query.trim().isEmpty());
        model.addAttribute("faqs", allFaqs);
        model.addAttribute("topFaqs", allFaqs);
        model.addAttribute("counts", counts);
        model.addAttribute("departments", faqService.getActiveDepartments());
        model.addAttribute("selectedDept", departmentId);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("totalCount", allFaqs.size());

        return "faqs";
    }
}

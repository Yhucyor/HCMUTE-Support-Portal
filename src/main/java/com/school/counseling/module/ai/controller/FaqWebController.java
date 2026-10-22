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

        boolean isSearchMode = query != null && !query.trim().isEmpty();
        List<FaqItemDto> faqs = isSearchMode 
                ? faqService.searchFaqsSmart(query, departmentId) 
                : faqService.getAllActiveFaqs();

        FaqCategoryCountDto counts = faqService.calculateCategoryCounts(faqs);

        SecurityUtils.getCurrentUser().ifPresent(currentUser -> {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("currentUserFullName", currentUser.getFullName());
            model.addAttribute("currentUserAvatar", currentUser.getAvatarUrl());
            String role = (currentUser.getRole() != null) ? currentUser.getRole().replace("ROLE_", "") : "STUDENT";
            model.addAttribute("currentUserRole", role);
        });

        model.addAttribute("isSearchMode", isSearchMode);
        model.addAttribute("faqs", faqs);
        model.addAttribute("topFaqs", faqs);
        model.addAttribute("counts", counts);
        model.addAttribute("departments", faqService.getActiveDepartments());
        model.addAttribute("selectedDept", departmentId);
        model.addAttribute("query", query);
        model.addAttribute("totalCount", counts.totalCount());

        return "faqs";
    }
}

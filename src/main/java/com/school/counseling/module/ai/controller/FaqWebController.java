package com.school.counseling.module.ai.controller;

import com.school.counseling.module.ai.entity.Faq;
import com.school.counseling.module.ai.repository.FaqRepository;
import com.school.counseling.module.ai.service.SmartFaqMatcherService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class FaqWebController {

    private final FaqRepository faqRepository;
    private final DepartmentRepository departmentRepository;
    private final SmartFaqMatcherService faqMatcherService;

    @GetMapping("/faqs")
    public String searchFaqs(
            @RequestParam(value = "q", required = false, defaultValue = "") String query,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            Model model) {

        boolean isSearchMode = query != null && !query.trim().isEmpty();
        List<SmartFaqMatcherService.FaqMatchResult> searchResults = List.of();
        List<Faq> topFaqs = List.of();

        if (isSearchMode) {
            // Tìm kiếm thông minh theo từ khóa không phân biệt dấu tiếng Việt và chuẩn hóa teencode
            searchResults = faqMatcherService.matchQuestion(query.trim(), departmentId);
        } else {
            // Trạng thái ban đầu: Hiển thị Top 15 câu hỏi quy chế phổ biến nhất
            if (departmentId != null) {
                topFaqs = faqRepository.findTopByDepartmentWithDepartment(departmentId, PageRequest.of(0, 15));
            } else {
                topFaqs = faqRepository.findTopActiveWithDepartment(PageRequest.of(0, 15));
            }
        }

        List<Department> departments = departmentRepository.findByIsActiveTrue();

        model.addAttribute("isSearchMode", isSearchMode);
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("topFaqs", topFaqs);
        model.addAttribute("departments", departments);
        model.addAttribute("selectedDept", departmentId);
        model.addAttribute("query", query);

        return "faq/search";
    }
}

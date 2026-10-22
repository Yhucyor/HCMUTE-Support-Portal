package com.school.counseling.module.ai.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.ai.entity.Faq;
import com.school.counseling.module.ai.repository.FaqRepository;
import com.school.counseling.module.ai.service.SmartFaqMatcherService;
import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class FaqWebController {

    private final FaqRepository faqRepository;
    private final DepartmentRepository departmentRepository;
    private final SmartFaqMatcherService faqMatcherService;

    public FaqWebController(FaqRepository faqRepository,
                            DepartmentRepository departmentRepository,
                            SmartFaqMatcherService faqMatcherService) {
        this.faqRepository = faqRepository;
        this.departmentRepository = departmentRepository;
        this.faqMatcherService = faqMatcherService;
    }

    @GetMapping({"/faqs", "/faq"})
    public String searchFaqs(
            @RequestParam(value = "q", required = false, defaultValue = "") String query,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            Model model) {

        boolean isSearchMode = query != null && !query.trim().isEmpty();
        List<SmartFaqMatcherService.FaqMatchResult> searchResults = List.of();
        List<Faq> faqs;

        if (isSearchMode) {
            // Tìm kiếm thông minh theo từ khóa không phân biệt dấu tiếng Việt và chuẩn hóa teencode
            searchResults = faqMatcherService.matchQuestion(query.trim(), departmentId);
            List<Faq> mappedFaqs = new java.util.ArrayList<>();
            for (SmartFaqMatcherService.FaqMatchResult r : searchResults) {
                Department dept = Department.builder()
                        .id(r.departmentId())
                        .name(r.departmentName())
                        .build();
                Faq f = Faq.builder()
                        .id(r.id())
                        .question(r.question())
                        .answer(r.answer())
                        .department(dept)
                        .category(r.category())
                        .postDate(r.postDate())
                        .build();
                mappedFaqs.add(f);
            }
            faqs = mappedFaqs;
        } else {
            if (departmentId != null) {
                faqs = faqRepository.findTopByDepartmentWithDepartment(departmentId, PageRequest.of(0, 100));
            } else {
                faqs = faqRepository.findAllActiveWithDepartment();
                if (faqs == null || faqs.isEmpty()) {
                    faqs = faqRepository.findTopActiveWithDepartment(PageRequest.of(0, 50));
                }
            }
        }

        List<Department> departments = departmentRepository.findByIsActiveTrue();

        UserPrincipal currentUser = SecurityUtils.getCurrentUser().orElse(null);
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("currentUserFullName", currentUser.getFullName());
            model.addAttribute("currentUserAvatar", currentUser.getAvatarUrl());
            String role = (currentUser.getRole() != null) ? currentUser.getRole().replace("ROLE_", "") : "STUDENT";
            model.addAttribute("currentUserRole", role);
        }

        model.addAttribute("isSearchMode", isSearchMode);
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("faqs", faqs);
        model.addAttribute("topFaqs", faqs);
        model.addAttribute("departments", departments);
        model.addAttribute("selectedDept", departmentId);
        model.addAttribute("query", query);
        model.addAttribute("totalCount", (faqs != null) ? faqs.size() : 0);

        return "faqs";
    }
}

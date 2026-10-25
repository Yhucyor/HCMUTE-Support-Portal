package com.school.counseling.module.ai.service;

import com.school.counseling.module.ai.dto.FaqCategoryCountDto;
import com.school.counseling.module.ai.dto.FaqItemDto;
import com.school.counseling.module.auth.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FaqService {
    List<FaqItemDto> getAllActiveFaqs();
    Page<FaqItemDto> getActiveFaqsPaged(Long departmentId, Pageable pageable);
    List<FaqItemDto> searchFaqsSmart(String query, Long departmentId);
    FaqCategoryCountDto calculateCategoryCounts(List<FaqItemDto> faqs);
    List<Department> getActiveDepartments();
    void incrementViewCount(Long faqId);
    void evictFaqCache();
}

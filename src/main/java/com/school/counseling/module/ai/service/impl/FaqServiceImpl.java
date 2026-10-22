package com.school.counseling.module.ai.service.impl;

import com.school.counseling.module.ai.dto.FaqCategoryCountDto;
import com.school.counseling.module.ai.dto.FaqItemDto;
import com.school.counseling.module.ai.entity.Faq;
import com.school.counseling.module.ai.repository.FaqRepository;
import com.school.counseling.module.ai.service.FaqService;
import com.school.counseling.module.ai.service.SmartFaqMatcherService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Tắt dirty checking mặc định cho toàn bộ truy vấn đọc
public class FaqServiceImpl implements FaqService {

    private final FaqRepository faqRepository;
    private final DepartmentRepository departmentRepository;
    private final SmartFaqMatcherService faqMatcherService;

    @Override
    @Cacheable(value = "faqs_all_active", unless = "#result == null || #result.isEmpty()")
    public List<FaqItemDto> getAllActiveFaqs() {
        log.debug("Đang tải toàn bộ FAQ kèm Department từ Database...");
        return faqRepository.findAllActiveWithDepartment().stream()
                .map(FaqItemDto::fromEntity)
                .toList();
    }

    @Override
    public Page<FaqItemDto> getActiveFaqsPaged(Long departmentId, Pageable pageable) {
        Page<Faq> page = (departmentId != null)
                ? faqRepository.findByDepartmentIdAndIsActiveTrue(departmentId, pageable)
                : faqRepository.findByIsActiveTrue(pageable);
        return page.map(FaqItemDto::fromEntity);
    }

    @Override
    public List<FaqItemDto> searchFaqsSmart(String query, Long departmentId) {
        if (query == null || query.isBlank()) {
            return getAllActiveFaqs();
        }
        var searchResults = faqMatcherService.matchQuestion(query.trim(), departmentId);
        return searchResults.stream()
                .map(r -> new FaqItemDto(
                        r.id(),
                        r.question(),
                        r.answer(),
                        r.departmentId(),
                        r.departmentName(),
                        r.category(),
                        FaqItemDto.resolveCategorySlug(r.category()),
                        10,
                        r.formattedDate() != null ? r.formattedDate() : "Mới cập nhật",
                        r.postDate() != null ? r.postDate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L
                ))
                .toList();
    }

    @Override
    public FaqCategoryCountDto calculateCategoryCounts(List<FaqItemDto> faqs) {
        if (faqs == null || faqs.isEmpty()) {
            return new FaqCategoryCountDto(0, 0, 0, 0, 0, 0, 0);
        }

        Map<String, Long> groupCounts = faqs.stream()
                .collect(Collectors.groupingBy(FaqItemDto::categorySlug, Collectors.counting()));

        return new FaqCategoryCountDto(
                faqs.size(),
                groupCounts.getOrDefault("hoc-vu", 0L),
                groupCounts.getOrDefault("hoc-phi", 0L),
                groupCounts.getOrDefault("quy-che", 0L),
                groupCounts.getOrDefault("ngoai-ngu", 0L),
                groupCounts.getOrDefault("tai-khoan", 0L),
                groupCounts.getOrDefault("dich-vu", 0L)
        );
    }

    @Override
    @Cacheable(value = "departments_active")
    public List<Department> getActiveDepartments() {
        return departmentRepository.findByIsActiveTrue();
    }

    @Override
    @Async("mailTaskExecutor")
    @Transactional
    public void incrementViewCount(Long faqId) {
        faqRepository.findById(faqId).ifPresent(f -> {
            f.setViewCount((f.getViewCount() == null ? 0 : f.getViewCount()) + 1);
            faqRepository.save(f);
        });
    }

    @Override
    @CacheEvict(value = {"faqs_all_active", "departments_active"}, allEntries = true)
    public void evictFaqCache() {
        log.info("Đã xóa trắng bộ nhớ cache của FAQ & Department");
    }
}

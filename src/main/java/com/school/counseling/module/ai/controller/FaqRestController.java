package com.school.counseling.module.ai.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.module.ai.service.FaqService;
import com.school.counseling.module.ai.service.SmartFaqMatcherService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/faqs")
@RequiredArgsConstructor
public class FaqRestController {

    private final SmartFaqMatcherService faqMatcherService;
    private final FaqService faqService;

    @GetMapping("/match")
    public ResponseEntity<ApiResponse<List<SmartFaqMatcherService.FaqMatchResult>>> matchQuestion(
            @RequestParam("q") @NotBlank(message = "Từ khóa tìm kiếm không được để trống") String query,
            @RequestParam(value = "departmentId", required = false) Long departmentId) {

        List<SmartFaqMatcherService.FaqMatchResult> results = faqMatcherService.matchQuestion(query, departmentId);
        return ResponseEntity.ok(ApiResponse.success(results, "Đối soát tri thức thành công"));
    }

    /**
     * Làm mới Cache RAM FAQ.
     * BẢO MẬT: Bắt buộc quyền ADMIN mới được phép kích hoạt để chống DoS.
     */
    @PostMapping("/reload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> reloadFaqCache() {
        faqMatcherService.reloadCache();
        faqService.evictFaqCache();
        return ResponseEntity.ok(ApiResponse.success("Đã làm mới bộ nhớ Cache FAQ thành công!", "OK"));
    }
}

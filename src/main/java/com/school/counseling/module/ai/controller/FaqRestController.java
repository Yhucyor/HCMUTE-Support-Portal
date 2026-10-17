package com.school.counseling.module.ai.controller;

import com.school.counseling.common.dto.ApiResponse;
import com.school.counseling.module.ai.service.SmartFaqMatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/faqs")
@RequiredArgsConstructor
public class FaqRestController {

    private final SmartFaqMatcherService faqMatcherService;

    @GetMapping("/match")
    public ResponseEntity<ApiResponse<List<SmartFaqMatcherService.FaqMatchResult>>> matchQuestion(
            @RequestParam("q") String query,
            @RequestParam(value = "departmentId", required = false) Long departmentId) {

        List<SmartFaqMatcherService.FaqMatchResult> results = faqMatcherService.matchQuestion(query, departmentId);
        return ResponseEntity.ok(ApiResponse.success(results, "Đối soát tri thức thành công"));
    }

    @PostMapping("/reload")
    public ResponseEntity<ApiResponse<String>> reloadFaqCache() {
        faqMatcherService.reloadCache();
        return ResponseEntity.ok(ApiResponse.success("Đã làm mới bộ nhớ Cache FAQ thành công!", "OK"));
    }
}

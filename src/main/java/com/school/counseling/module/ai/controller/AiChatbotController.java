package com.school.counseling.module.ai.controller;

import com.school.counseling.module.ai.dto.AiChatRequest;
import com.school.counseling.module.ai.dto.AiChatResponse;
import com.school.counseling.module.ai.service.AiChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatbotController {

    private final AiChatbotService aiChatbotService;

    public AiChatbotController(AiChatbotService aiChatbotService) {
        this.aiChatbotService = aiChatbotService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        AiChatResponse response = aiChatbotService.getChatResponse(
                request.getQuery(),
                request.getSessionId(), // Dùng camelCase mới
                request.getProvider());
        return ResponseEntity.ok(response);
    }
}

package com.school.counseling.module.ai.service;

import com.school.counseling.module.ai.dto.AiChatRequest;
import com.school.counseling.module.ai.dto.AiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiChatbotService {

    @Value("${ai.chatbot.url:http://localhost:8000/chat}")
    private String chatbotApiUrl;

    private final RestTemplate restTemplate;

    public AiChatbotService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public AiChatResponse getChatResponse(String query, String sessionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AiChatRequest requestPayload = new AiChatRequest(query, sessionId);
        HttpEntity<AiChatRequest> requestEntity = new HttpEntity<>(requestPayload, headers);

        try {
            ResponseEntity<AiChatResponse> responseEntity = restTemplate.postForEntity(
                    chatbotApiUrl, 
                    requestEntity, 
                    AiChatResponse.class
            );
            return responseEntity.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            AiChatResponse errorResponse = new AiChatResponse();
            errorResponse.setAnswer("Xin lỗi, hệ thống AI hiện không thể phản hồi. Vui lòng thử lại sau.");
            errorResponse.setSession_id(sessionId);
            return errorResponse;
        }
    }
}

package com.school.counseling.module.ai.controller;

import com.school.counseling.module.ai.entity.Conversation;
import com.school.counseling.module.ai.service.AiConversationService;
import com.school.counseling.module.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Web Controller cho AI Chatbot UI
 * Phase 3: Render chatbot page với SSE streaming
 */
@Controller
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatWebController {

    private final AiConversationService conversationService;

    /**
     * Trang chính AI Chatbot
     */
    @GetMapping("/chat")
    public String chatPage(
            @AuthenticationPrincipal User currentUser,
            Model model) {
        
        if (currentUser != null) {
            // Load recent conversations cho sidebar
            List<Conversation> recentConversations = conversationService.getUserConversations(currentUser.getId());
            model.addAttribute("recentConversations", recentConversations);
            model.addAttribute("currentUser", currentUser);
        }
        
        model.addAttribute("pageTitle", "QAUTE - Trợ Lý AI HCMUTE");
        return "ai-chat";
    }

    /**
     * Load lại conversation cũ
     */
    @GetMapping("/chat/{sessionId}")
    public String loadConversation(
            @PathVariable String sessionId,
            @AuthenticationPrincipal User currentUser,
            Model model) {
        
        Conversation conversation = conversationService.getConversationBySessionId(sessionId);
        
        if (currentUser != null) {
            List<Conversation> recentConversations = conversationService.getUserConversations(currentUser.getId());
            model.addAttribute("recentConversations", recentConversations);
            model.addAttribute("currentUser", currentUser);
        }
        
        model.addAttribute("conversation", conversation);
        model.addAttribute("messages", conversation.getMessages());
        model.addAttribute("pageTitle", conversation.getTitle());
        
        return "ai-chat";
    }
}

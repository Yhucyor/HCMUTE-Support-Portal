package com.school.counseling.module.ai.controller;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.ai.entity.AiChatMessage;
import com.school.counseling.module.ai.entity.Conversation;
import com.school.counseling.module.ai.service.AiConversationService;
import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Web Controller cho AI Chatbot UI
 * Phase 3: Render chatbot page với SSE streaming
 * Hỗ trợ cả 2 đường dẫn thân thiện /ai/chat và /ai-chat
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AiChatWebController {

    private final AiConversationService conversationService;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * Lấy thực thể User hiện tại từ SecurityContext
     */
    private User resolveCurrentUser() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElse(null);
    }

    /**
     * Nạp các thông tin người dùng và phòng ban vào Model
     */
    private void populateCommonModel(Model model, User currentUser) {
        UserPrincipal userPrincipal = SecurityUtils.getCurrentUser().orElse(null);
        if (userPrincipal != null) {
            model.addAttribute("currentUser", userPrincipal);
            model.addAttribute("currentUserFullName", userPrincipal.getFullName());
            model.addAttribute("currentUserAvatar", userPrincipal.getAvatarUrl());
            String role = (userPrincipal.getRole() != null) ? userPrincipal.getRole().replace("ROLE_", "") : "STUDENT";
            model.addAttribute("currentUserRole", role);
            model.addAttribute("currentUserDept", userPrincipal.getDepartmentName() != null ? userPrincipal.getDepartmentName() : "Toàn trường");
        } else if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("currentUserFullName", currentUser.getFullName());
            model.addAttribute("currentUserAvatar", currentUser.getAvatarUrl());
            String role = (currentUser.getRole() != null) ? currentUser.getRole().getName().replace("ROLE_", "") : "STUDENT";
            model.addAttribute("currentUserRole", role);
            model.addAttribute("currentUserDept", currentUser.getDepartment() != null ? currentUser.getDepartment().getName() : "Toàn trường");
        }
        model.addAttribute("departments", departmentRepository.findByIsActiveTrue());
    }

    /**
     * Trang chính AI Chatbot (hỗ trợ cả /ai/chat và /ai-chat)
     */
    @GetMapping({"/ai/chat", "/ai-chat"})
    public String chatPage(Model model) {
        User currentUser = resolveCurrentUser();
        populateCommonModel(model, currentUser);
        
        if (currentUser != null) {
            List<Conversation> recentConversations = conversationService.getUserConversations(currentUser.getId());
            model.addAttribute("recentConversations", recentConversations);
            log.info("Loaded {} conversations for user ID: {}", recentConversations.size(), currentUser.getId());
        }
        
        model.addAttribute("pageTitle", "QAUTE - Trợ Lý AI HCMUTE");
        return "ai-chat";
    }

    /**
     * Load lại conversation cũ theo sessionId
     */
    @GetMapping({"/ai/chat/{sessionId}", "/ai-chat/{sessionId}"})
    public String loadConversation(
            @PathVariable String sessionId,
            Model model) {
        
        User currentUser = resolveCurrentUser();
        populateCommonModel(model, currentUser);
        
        Conversation conversation;
        try {
            conversation = conversationService.getConversationBySessionId(sessionId);
        } catch (Exception e) {
            log.warn("Conversation not found for sessionId {}: {}", sessionId, e.getMessage());
            return "redirect:/ai/chat";
        }
        
        if (currentUser != null) {
            List<Conversation> recentConversations = conversationService.getUserConversations(currentUser.getId());
            model.addAttribute("recentConversations", recentConversations);
        }
        
        // Truy vấn messages tường minh qua service để tránh lỗi LazyInitializationException
        List<AiChatMessage> messages = conversationService.getConversationMessages(conversation.getId());
        
        model.addAttribute("conversation", conversation);
        model.addAttribute("messages", messages);
        model.addAttribute("pageTitle", conversation.getTitle() != null ? conversation.getTitle() : "Cuộc trò chuyện");
        
        return "ai-chat";
    }
}

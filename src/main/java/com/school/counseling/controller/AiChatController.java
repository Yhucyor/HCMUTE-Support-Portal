package com.school.counseling.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AiChatController {

    @GetMapping("/ai-chat")
    public String aiChatPage() {
        return "ai-chat";
    }
}

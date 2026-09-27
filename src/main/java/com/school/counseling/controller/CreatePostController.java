package com.school.counseling.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CreatePostController {

    @GetMapping("/post/create")
    public String createPostPage() {
        return "create-post";
    }
}

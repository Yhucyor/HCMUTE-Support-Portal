package com.school.counseling.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NewsController {

    @GetMapping("/news/{id}")
    public String newsDetail(@PathVariable("id") Long id, Model model) {
        // Trong dự án thực tế, id này sẽ được dùng để truy vấn nội dung bài viết từ CSDL
        model.addAttribute("newsId", id);
        return "news-detail";
    }
}

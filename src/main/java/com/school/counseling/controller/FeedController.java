package com.school.counseling.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FeedController {

    @GetMapping("/feed")
    public String feedPage() {
        return "feed";
    }
}

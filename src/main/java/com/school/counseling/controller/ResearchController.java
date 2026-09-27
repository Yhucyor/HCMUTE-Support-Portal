package com.school.counseling.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ResearchController {

    @GetMapping("/research")
    public String researchPage() {
        return "research";
    }
}

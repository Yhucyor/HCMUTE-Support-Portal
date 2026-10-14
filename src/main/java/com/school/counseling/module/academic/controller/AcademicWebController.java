package com.school.counseling.module.academic.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/academic")
public class AcademicWebController {

    @GetMapping("/sync")
    public String syncPage(Model model) {
        return "academic-sync";
    }
}

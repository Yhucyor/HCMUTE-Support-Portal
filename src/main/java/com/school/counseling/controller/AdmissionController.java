package com.school.counseling.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdmissionController {

    @GetMapping("/admission")
    public String admissionPage() {
        return "admission";
    }
}

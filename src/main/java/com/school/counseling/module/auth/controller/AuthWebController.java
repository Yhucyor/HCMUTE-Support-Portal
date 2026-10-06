package com.school.counseling.module.auth.controller;

import com.school.counseling.common.exception.AppException;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.dto.RegisterRequest;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthWebController {

    private final AuthService authService;
    private final DepartmentRepository departmentRepository;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (SecurityUtils.isAuthenticated()) {
            return "redirect:/home";
        }
        if (error != null) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất thành công khỏi hệ thống.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (SecurityUtils.isAuthenticated()) {
            return "redirect:/home";
        }
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        model.addAttribute("departments", departmentRepository.findByIsActiveTrue());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentRepository.findByIsActiveTrue());
            return "register";
        }

        try {
            authService.register(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký tài khoản thành công! Vui lòng đăng nhập để tiếp tục.");
            return "redirect:/auth/login";
        } catch (AppException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("departments", departmentRepository.findByIsActiveTrue());
            return "register";
        } catch (Exception ex) {
            log.error("Registration failed", ex);
            model.addAttribute("errorMessage", "Đã có lỗi xảy ra trong quá trình đăng ký: " + ex.getMessage());
            model.addAttribute("departments", departmentRepository.findByIsActiveTrue());
            return "register";
        }
    }
}

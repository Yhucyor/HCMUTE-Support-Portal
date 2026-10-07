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

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(@RequestParam(value = "email", required = false) String email,
                                     @RequestParam(value = "step", defaultValue = "1") int step,
                                     Model model) {
        if (SecurityUtils.isAuthenticated()) {
            return "redirect:/home";
        }
        model.addAttribute("email", email);
        model.addAttribute("step", step);
        return "forgot-password";
    }

    @PostMapping("/forgot-password/send-otp")
    public String handleSendOtp(@RequestParam("email") String email,
                                RedirectAttributes redirectAttributes) {
        if (email == null || email.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập địa chỉ email của bạn");
            return "redirect:/auth/forgot-password?step=1";
        }

        try {
            authService.forgotPassword(email.trim());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Mã xác thực OTP đã được gửi đến email " + email.trim() + ". Vui lòng kiểm tra hộp thư (và thư mục Spam).");
            return "redirect:/auth/forgot-password?step=2&email="
                    + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (AppException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/forgot-password?step=1";
        } catch (Exception ex) {
            log.error("Failed to send OTP", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể gửi mã OTP. Vui lòng thử lại sau!");
            return "redirect:/auth/forgot-password?step=1";
        }
    }

    @PostMapping("/forgot-password/reset")
    public String handleResetPassword(@RequestParam("email") String email,
                                      @RequestParam("otpCode") String otpCode,
                                      @RequestParam("newPassword") String newPassword,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            return "redirect:/auth/forgot-password?step=2&email="
                    + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        }

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới phải có ít nhất 6 ký tự!");
            return "redirect:/auth/forgot-password?step=2&email="
                    + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        }

        try {
            authService.resetPassword(email.trim(), otpCode.trim(), newPassword);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
            return "redirect:/auth/login";
        } catch (AppException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/forgot-password?step=2&email="
                    + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("Reset password failed", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi đặt lại mật khẩu!");
            return "redirect:/auth/forgot-password?step=2&email="
                    + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}

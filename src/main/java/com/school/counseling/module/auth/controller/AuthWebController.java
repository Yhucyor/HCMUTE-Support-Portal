package com.school.counseling.module.auth.controller;

import com.school.counseling.common.exception.AppException;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.dto.RegisterRequest;
import com.school.counseling.module.auth.service.AuthService;
import com.school.counseling.module.auth.service.DepartmentQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
public class AuthWebController {

    private final AuthService authService;
    private final DepartmentQueryService departmentQueryService;

    public AuthWebController(AuthService authService, DepartmentQueryService departmentQueryService) {
        this.authService = authService;
        this.departmentQueryService = departmentQueryService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            HttpServletRequest request,
                            Model model) {
        request.getSession(true);
        if (SecurityUtils.isAuthenticated()) {
            return "redirect:/home";
        }
        if (error != null) {
            if ("oauth2".equalsIgnoreCase(error)) {
                Object oauthEx = request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
                String msg = (oauthEx instanceof Exception e && e.getMessage() != null)
                        ? e.getMessage()
                        : "Đăng nhập bằng tài khoản Google không thành công. Vui lòng thử lại!";
                model.addAttribute("errorMessage", msg);
            } else {
                Object lastException = request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
                if (lastException instanceof org.springframework.security.authentication.DisabledException) {
                    model.addAttribute("errorMessage", "Tài khoản của bạn chưa được kích hoạt qua mã OTP! Vui lòng hoàn tất kích hoạt tài khoản để đăng nhập.");
                } else if (lastException instanceof org.springframework.security.authentication.LockedException) {
                    model.addAttribute("errorMessage", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên.");
                } else {
                    model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");
                }
            }
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất thành công khỏi hệ thống.");
        }
        return "auth/login";
    }

    @PostMapping("/portal-login")
    public String handlePortalLogin(@RequestParam("studentId") String studentId,
                                    @RequestParam("portalPassword") String portalPassword,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        try {
            authService.loginWithHcmutePortal(studentId, portalPassword, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng nhập thành công qua Cổng Đào tạo HCMUTE! Hệ thống đang đồng bộ dữ liệu học vụ của bạn.");
            return "redirect:/home";
        } catch (AppException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        } catch (Exception ex) {
            log.error("Portal login failed", ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Đăng nhập qua Cổng Đào tạo thất bại: " + ex.getMessage());
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(HttpServletRequest request, Model model) {
        request.getSession(true);
        if (SecurityUtils.isAuthenticated()) {
            return "redirect:/home";
        }
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        model.addAttribute("departments", departmentQueryService.getActiveDepartments());
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentQueryService.getActiveDepartments());
            return "auth/register";
        }

        try {
            authService.register(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng ký thành công! Mã OTP xác thực 6 chữ số đã được gửi tới email " + request.getEmail().trim()
                            + ". Vui lòng nhập mã để kích hoạt tài khoản.");
            return "redirect:/auth/activate?email="
                    + java.net.URLEncoder.encode(request.getEmail().trim(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (AppException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("departments", departmentQueryService.getActiveDepartments());
            return "auth/register";
        } catch (Exception ex) {
            log.error("Registration failed", ex);
            model.addAttribute("errorMessage", "Đã có lỗi xảy ra trong quá trình đăng ký: " + ex.getMessage());
            model.addAttribute("departments", departmentQueryService.getActiveDepartments());
            return "auth/register";
        }
    }

    @GetMapping("/activate")
    public String activatePage(@RequestParam(value = "email", required = false) String email,
                               HttpServletRequest request,
                               Model model) {
        request.getSession(true);
        if (SecurityUtils.isAuthenticated()) {
            return "redirect:/home";
        }
        model.addAttribute("email", email);
        return "auth/activate";
    }

    @PostMapping("/activate")
    public String handleActivate(@RequestParam("email") String email,
                                 @RequestParam("otpCode") String otpCode,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (email == null || email.isBlank() || otpCode == null || otpCode.isBlank()) {
            model.addAttribute("errorMessage", "Vui lòng nhập đầy đủ Email và Mã OTP 6 chữ số.");
            model.addAttribute("email", email);
            return "auth/activate";
        }

        try {
            authService.activateAccount(email.trim(), otpCode.trim());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ.");
            return "redirect:/auth/login";
        } catch (AppException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("email", email.trim());
            return "auth/activate";
        } catch (Exception ex) {
            log.error("Activation failed", ex);
            model.addAttribute("errorMessage", "Không thể kích hoạt tài khoản. Vui lòng thử lại!");
            model.addAttribute("email", email.trim());
            return "auth/activate";
        }
    }

    @PostMapping("/resend-activation-otp")
    public String handleResendActivationOtp(@RequestParam("email") String email,
                                            RedirectAttributes redirectAttributes) {
        if (email == null || email.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng cung cấp địa chỉ email hợp lệ");
            return "redirect:/auth/activate";
        }

        try {
            authService.resendActivationOtp(email.trim());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Mã OTP kích hoạt mới đã được gửi đến email " + email.trim() + ". Vui lòng kiểm tra hộp thư!");
            return "redirect:/auth/activate?email="
                    + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (AppException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/activate?email="
                    + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("Resend activation OTP failed", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể gửi lại mã OTP. Vui lòng thử lại sau!");
            return "redirect:/auth/activate?email="
                    + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(@RequestParam(value = "email", required = false) String email,
                                     @RequestParam(value = "step", defaultValue = "1") int step,
                                     @RequestParam(value = "otpCode", required = false) String otpCode,
                                     HttpServletRequest request,
                                     Model model) {
        request.getSession(true);
        if (SecurityUtils.isAuthenticated()) {
            return "redirect:/home";
        }
        model.addAttribute("email", email);
        model.addAttribute("step", step);
        model.addAttribute("otpCode", otpCode);
        if (step == 2 || step == 3) {
            return "auth/reset-password";
        }
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "email", required = false) String email,
                                    @RequestParam(value = "otpCode", required = false) String otpCode,
                                    HttpServletRequest request,
                                    Model model) {
        request.getSession(true);
        if (SecurityUtils.isAuthenticated()) {
            return "redirect:/home";
        }
        model.addAttribute("email", email);
        model.addAttribute("otpCode", otpCode);
        return "auth/reset-password";
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

    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response,
                         org.springframework.security.core.Authentication authentication) {
        if (authentication != null) {
            new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/?logout=true";
    }
}

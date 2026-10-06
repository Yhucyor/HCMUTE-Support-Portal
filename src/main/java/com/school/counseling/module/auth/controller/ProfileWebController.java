package com.school.counseling.module.auth.controller;

import com.school.counseling.common.exception.AppException;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.dto.ChangePasswordRequest;
import com.school.counseling.module.auth.dto.UpdateProfileRequest;
import com.school.counseling.module.auth.dto.UserProfileDto;
import com.school.counseling.module.auth.dto.UserPrincipal;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProfileWebController {

    private final ProfileService profileService;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public String viewProfile(Model model) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException("Vui lòng đăng nhập để xem thông tin"));

        UserProfileDto profile = profileService.getProfile(currentUser.getId());
        model.addAttribute("user", profile);
        model.addAttribute("departments", departmentRepository.findByIsActiveTrue());

        if (!model.containsAttribute("updateProfileRequest")) {
            UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                    .fullName(profile.getFullName())
                    .email(profile.getEmail())
                    .phone(profile.getPhone())
                    .departmentId(profile.getDepartmentId())
                    .build();
            model.addAttribute("updateProfileRequest", updateRequest);
        }

        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }

        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("updateProfileRequest") UpdateProfileRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.updateProfileRequest", bindingResult);
            redirectAttributes.addFlashAttribute("updateProfileRequest", request);
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin cập nhật không hợp lệ!");
            return "redirect:/profile";
        }

        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AppException("Chưa đăng nhập"));

        try {
            profileService.updateProfile(currentUserId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ cá nhân thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.changePasswordRequest", bindingResult);
            redirectAttributes.addFlashAttribute("changePasswordRequest", request);
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin đổi mật khẩu không hợp lệ!");
            return "redirect:/profile";
        }

        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AppException("Chưa đăng nhập"));

        try {
            profileService.changePassword(currentUserId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file ảnh hợp lệ");
            return "redirect:/profile";
        }

        Long currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AppException("Chưa đăng nhập"));

        try {
            profileService.updateAvatar(currentUserId, file);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật ảnh đại diện thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tải ảnh thất bại: " + ex.getMessage());
        }

        return "redirect:/profile";
    }
}

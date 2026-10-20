package com.school.counseling.config;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.dto.UserAccountMenuDto;
import com.school.counseling.module.auth.service.UserAccountMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global Controller Advice
 * Inject common model attributes vào tất cả các Controller
 * Bao gồm: userAccountMenu cho User Account Area dropdown
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {
    
    private final UserAccountMenuService userAccountMenuService;
    
    /**
     * Inject userAccountMenu vào tất cả các View
     * Dùng cho User Account Area dropdown trên Topbar
     * Chỉ hiển thị khi user đã authenticated
     */
    @ModelAttribute("userAccountMenu")
    public UserAccountMenuDto userAccountMenu() {
        if (!SecurityUtils.isAuthenticated()) {
            return null;
        }
        
        try {
            return userAccountMenuService.buildUserAccountMenu();
        } catch (Exception ex) {
            log.warn("Failed to build userAccountMenu: {}", ex.getMessage());
            return null;
        }
    }
    
    /**
     * Helper method check xem user hiện tại có phải STUDENT không
     * Dùng cho conditional rendering menu items trong View
     */
    @ModelAttribute("isStudent")
    public boolean isStudent() {
        return SecurityUtils.hasRole("ROLE_STUDENT");
    }
    
    /**
     * Helper method check xem user hiện tại có phải STAFF không
     */
    @ModelAttribute("isStaff")
    public boolean isStaff() {
        return SecurityUtils.hasRole("ROLE_STAFF");
    }
    
    /**
     * Helper method check xem user hiện tại có phải ADMIN không
     */
    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        return SecurityUtils.hasRole("ROLE_ADMIN");
    }
}

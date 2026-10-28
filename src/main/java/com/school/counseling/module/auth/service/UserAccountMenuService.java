package com.school.counseling.module.auth.service;

import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.dto.UserAccountMenuDto;
import com.school.counseling.module.auth.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service xử lý User Account Area data
 * Chịu trách nhiệm lấy thông tin user từ Security Context
 * và build UserAccountMenuDto cho View
 */
@Slf4j
@Service
public class UserAccountMenuService {
    
    /**
     * Build UserAccountMenuDto từ current authenticated user
     * Timeout 200ms theo requirement 8
     * 
     * @return UserAccountMenuDto với thông tin user hoặc null nếu chưa đăng nhập
     */
    public UserAccountMenuDto buildUserAccountMenu() {
        try {
            UserPrincipal currentUser = SecurityUtils.getCurrentUser().orElse(null);
            
            if (currentUser == null) {
                return null;
            }
            
            // Build DTO thuần in-memory từ UserPrincipal (không query DB)
            return UserAccountMenuDto.builder()
                    .fullName(currentUser.getFullName())
                    .username(currentUser.getUsername())
                    .avatarUrl(currentUser.getAvatarUrl())
                    .role(extractRoleName(currentUser.getRole()))
                    .build();
            
        } catch (Exception ex) {
            log.error("Error building UserAccountMenu: {}", ex.getMessage(), ex);
            return null;
        }
    }
    
    /**
     * Extract role name (loại bỏ prefix ROLE_)
     */
    private String extractRoleName(String roleWithPrefix) {
        if (roleWithPrefix == null) {
            return "STUDENT";
        }
        return roleWithPrefix.replace("ROLE_", "");
    }
    
    /**
     * Kiểm tra user hiện tại có phải STUDENT không
     */
    public boolean isCurrentUserStudent() {
        return SecurityUtils.hasRole("ROLE_STUDENT");
    }
}

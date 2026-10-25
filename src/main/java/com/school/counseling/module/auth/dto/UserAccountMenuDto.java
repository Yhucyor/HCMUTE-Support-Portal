package com.school.counseling.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho User Account Area dropdown menu
 * Chứa thông tin hiển thị trên Topbar: avatar, fullName, username (MSSV)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountMenuDto {
    
    private String fullName;
    private String username; // MSSV cho sinh viên
    private String avatarUrl;
    private String role; // STUDENT, STAFF, ADMIN
    
    /**
     * Tạo generated avatar từ chữ cái đầu của tên
     * Dùng khi avatarUrl null hoặc rỗng
     */
    public String getAvatarInitial() {
        if (fullName == null || fullName.isBlank()) {
            return "S"; // Default "Sinh viên"
        }
        return String.valueOf(fullName.trim().charAt(0)).toUpperCase();
    }
    
    /**
     * Kiểm tra có avatar URL hợp lệ không
     */
    public boolean hasAvatar() {
        return avatarUrl != null && !avatarUrl.isBlank();
    }
    
    /**
     * Lấy display name hiển thị (fallback "Sinh viên" nếu null)
     */
    public String getDisplayName() {
        if (fullName == null || fullName.isBlank()) {
            return "Sinh viên";
        }
        return fullName;
    }
}

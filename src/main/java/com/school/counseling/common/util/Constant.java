package com.school.counseling.common.util;

import java.io.File;

/**
 * Hằng số cấu hình hệ thống (Tương thích với giáo trình & bài tập lớn STS)
 */
public final class Constant {
    private Constant() {}

    // Thư mục lưu trữ ảnh và file upload cục bộ trên máy tính
    public static final String DIR = "C:" + File.separator + "upload";
    
    // Các thư mục con
    public static final String DIR_IMAGES = DIR + File.separator + "images";
    public static final String DIR_DOCUMENTS = DIR + File.separator + "documents";
    public static final String DIR_VIDEOS = DIR + File.separator + "videos";

    // Phân quyền Role Name
    public static final String ROLE_STUDENT = "ROLE_STUDENT";
    public static final String ROLE_STAFF = "ROLE_STAFF";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
}

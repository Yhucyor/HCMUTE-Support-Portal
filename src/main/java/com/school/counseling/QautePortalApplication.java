package com.school.counseling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class QautePortalApplication {

    public static void main(String[] args) {
        // Tự động khởi tạo các thư mục upload cục bộ C:/upload nếu chưa tồn tại
        initUploadDirectories();
        SpringApplication.run(QautePortalApplication.class, args);
    }

    private static void initUploadDirectories() {
        try {
            String[] dirs = {
                "C:/upload",
                "C:/upload/images",
                "C:/upload/documents",
                "C:/upload/videos"
            };
            for (String d : dirs) {
                File dir = new File(d);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
            }
        } catch (Exception ignored) {
            // Khi chạy trên môi trường không có quyền ghi ổ C, hệ thống fallback sang thư mục tương đối
        }
    }
}

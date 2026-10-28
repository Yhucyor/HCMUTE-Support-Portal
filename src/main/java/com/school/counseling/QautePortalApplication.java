package com.school.counseling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// kich hoat web chay 
import org.springframework.scheduling.annotation.EnableAsync;
// chay duoi nen ngam 
import org.springframework.scheduling.annotation.EnableScheduling;
// hen gio tu dong don rac 

import java.io.File; // cong cu kt tep tin va thu muc 

@SpringBootApplication
// bao file cao nhat khoi dong toan bo he thong web tu day
@EnableAsync // kich hoat tinh năng chay bat dong bo
@EnableScheduling // bật tính năng chạy hen gio tu dong
public class QautePortalApplication {

    public static void main(String[] args) {
        // Tự động khởi tạo các thư mục upload cục bộ D:/portal/upload nếu chưa tồn tại
        initUploadDirectories(); // kt va tao san cac thu muc luu anh video tren may
        SpringApplication.run(QautePortalApplication.class, args); // bat cong tac khoi dong web ,
        // bat may chu noi bo len thuong o 8080 , de b co the mo trinh duyet go vao xem
        // web

    }

    // ham tu tao thu muc luu tru
    private static void initUploadDirectories() {
        try {
            String[] dirs = {
                    "D:/portal/upload",
                    "D:/portal/upload/images",
                    "D:/portal/upload/documents",
                    "D:/portal/upload/videos"
            };
            // vong lap duyet qua 4 duong dan
            for (String d : dirs) {
                File dir = new File(d); // trỏ tới đường dẫn thư mục đó
                if (!dir.exists()) {
                    dir.mkdirs(); // nếu chưa tồn tại thì tạo
                }
            }
            // catch bắt lỗi nếu có sự cố xảy ra
        } catch (Exception ignored) {
            // Khi chạy trên môi trường không có quyền ghi ổ C, hệ thống fallback sang thư
            // mục tương đối

        }
    }
}

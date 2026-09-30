package com.school.counseling.common;

import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Tiện ích kiểm tra tính hợp lệ và an toàn của tệp tin tải lên (File Upload Validation) trong QAUTE Portal.
 */
public final class FileUploadValidator {

    // Kích thước tối đa mặc định: 15 MB
    public static final long DEFAULT_MAX_SIZE = 15 * 1024 * 1024;

    // Danh sách định dạng tệp tin cho phép
    public static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");
    public static final List<String> ALLOWED_DOC_EXTENSIONS = Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip");
    public static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList("mp4", "webm");

    private FileUploadValidator() {
        // Utility class
    }

    /**
     * Lấy phần mở rộng (extension) của tệp tin.
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ENGLISH);
    }

    /**
     * Làm sạch tên tệp tin để chống tấn công Path Traversal (VD: ../../etc/passwd) và ký tự nguy hiểm.
     */
    public static String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "unnamed_file";
        }

        // Chỉ lấy tên file, loại bỏ đường dẫn thư mục
        String cleanName = originalFilename.replaceAll("[\\\\/]+", "_");
        cleanName = cleanName.replaceAll("[^a-zA-Z0-9._-]", "_");

        return cleanName;
    }

    /**
     * Kiểm tra tệp tin ảnh hợp lệ (Dung lượng + Định dạng đuôi).
     */
    public static void validateImage(MultipartFile file, long maxSizeBytes) {
        validateCommon(file, maxSizeBytes);
        String ext = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new AppException(400, "Định dạng ảnh không được hỗ trợ! Chỉ chấp nhận: " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }
    }

    /**
     * Kiểm tra tệp tin tài liệu hoặc đính kèm Ticket hợp lệ.
     */
    public static void validateAttachment(MultipartFile file, long maxSizeBytes) {
        validateCommon(file, maxSizeBytes);
        String ext = getFileExtension(file.getOriginalFilename());

        boolean isAllowed = ALLOWED_IMAGE_EXTENSIONS.contains(ext)
                || ALLOWED_DOC_EXTENSIONS.contains(ext)
                || ALLOWED_VIDEO_EXTENSIONS.contains(ext);

        if (!isAllowed) {
            throw new AppException(400, "Định dạng tệp đính kèm không hợp lệ! Đuôi tệp: ." + ext);
        }
    }

    private static void validateCommon(MultipartFile file, long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new AppException(400, "Vui lòng chọn tệp tin để tải lên!");
        }

        if (file.getSize() > maxSizeBytes) {
            long maxMb = maxSizeBytes / (1024 * 1024);
            throw new AppException(413, String.format("Dung lượng tệp vượt quá giới hạn cho phép (%d MB)!", maxMb));
        }
    }
}

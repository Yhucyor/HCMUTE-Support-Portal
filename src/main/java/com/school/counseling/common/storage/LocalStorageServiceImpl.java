package com.school.counseling.common.storage;

import com.school.counseling.common.exception.InvalidFileException;
import com.school.counseling.common.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service("localStorageService")
public class LocalStorageServiceImpl implements IStorageService {

    private final Tika tika = new Tika();

    @Override
    public StorageResult uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Tệp tải lên không được rỗng");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        long fileSize = file.getSize();

        // 1. Kiểm tra dung lượng tối đa 100MB
        if (fileSize > 100 * 1024 * 1024) {
            throw new InvalidFileException("Dung lượng tệp vượt quá giới hạn 100MB");
        }

        try {
            // 2. Kiểm tra Magic Bytes thực tế
            String detectedMimeType = tika.detect(file.getInputStream());
            String fileType = resolveFileType(detectedMimeType, originalFilename);

            // 3. Chuan bi thu muc luu tru C:/upload/{folder}
            String targetDir = Constant.DIR;
            if ("images".equalsIgnoreCase(folder) || "IMAGE".equalsIgnoreCase(fileType)) {
                targetDir = Constant.DIR_IMAGES;
            } else if ("videos".equalsIgnoreCase(folder) || "MP4".equalsIgnoreCase(fileType)) {
                targetDir = Constant.DIR_VIDEOS;
            } else if ("documents".equalsIgnoreCase(folder) || "PDF".equalsIgnoreCase(fileType) || "DOCX".equalsIgnoreCase(fileType) || "XLSX".equalsIgnoreCase(fileType)) {
                targetDir = Constant.DIR_DOCUMENTS;
            }

            File dir = new File(targetDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 4. Đặt tên file duy nhất tránh trùng lặp
            String extension = getExtension(originalFilename);
            String storedFileName = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
            Path destination = Paths.get(targetDir, storedFileName);

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            // 5. Tạo đường dẫn public URL tương thích STS DownloadImageController
            String publicUrl = getFileUrl(storedFileName, fileType);
            log.info("Lưu tệp thành công: {} -> {}", originalFilename, destination.toAbsolutePath());

            return StorageResult.success(originalFilename, storedFileName, publicUrl, fileType, fileSize);
        } catch (IOException e) {
            log.error("Lỗi khi lưu tệp tin: ", e);
            throw new InvalidFileException("Không thể lưu trữ tệp: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        if (!StringUtils.hasText(fileName)) return false;
        try {
            File file = new File(Constant.DIR_IMAGES, fileName);
            if (!file.exists()) file = new File(Constant.DIR_DOCUMENTS, fileName);
            if (!file.exists()) file = new File(Constant.DIR_VIDEOS, fileName);
            if (!file.exists()) file = new File(Constant.DIR, fileName);

            return file.exists() && file.delete();
        } catch (Exception e) {
            log.warn("Không thể xóa tệp: {}", fileName);
            return false;
        }
    }

    @Override
    public String getFileUrl(String fileName, String fileType) {
        if ("IMAGE".equalsIgnoreCase(fileType)) {
            return "/image?fname=" + fileName;
        }
        return "/documents/" + fileName;
    }

    private String resolveFileType(String mimeType, String filename) {
        if (mimeType.startsWith("image/")) return "IMAGE";
        if (mimeType.equals("application/pdf")) return "PDF";
        if (mimeType.equals("video/mp4")) return "MP4";
        if (mimeType.contains("word") || filename.endsWith(".docx") || filename.endsWith(".doc")) return "DOCX";
        if (mimeType.contains("excel") || mimeType.contains("spreadsheet") || filename.endsWith(".xlsx") || filename.endsWith(".xls")) return "XLSX";
        return "DOCUMENT";
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0 && dotIndex < filename.length() - 1) ? filename.substring(dotIndex + 1) : "";
    }
}

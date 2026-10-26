package com.school.counseling.common.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.school.counseling.common.exception.InvalidFileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service("cloudinaryStorageService")
@Primary
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements IStorageService {

    private final Cloudinary cloudinary;
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
            // 2. Nhận diện MIME Type
            String detectedMimeType = tika.detect(file.getInputStream());
            String fileType = resolveFileType(detectedMimeType, originalFilename);

            // 3. Chuẩn bị thư mục trên Cloudinary: qaute_portal/{folder}
            String targetFolder = "qaute_portal/" + (StringUtils.hasText(folder) ? folder : "misc");

            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", targetFolder,
                    "resource_type", "auto",
                    "use_filename", true,
                    "unique_filename", true
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), params);

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            log.info("Tải lên Cloudinary thành công: {} -> {}", originalFilename, secureUrl);

            return StorageResult.success(originalFilename, publicId, secureUrl, fileType, fileSize);
        } catch (IOException e) {
            log.error("Lỗi khi tải tệp lên Cloudinary: ", e);
            throw new InvalidFileException("Không thể tải tệp lên Cloudinary: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String fileIdentifier) {
        if (!StringUtils.hasText(fileIdentifier)) return false;
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(fileIdentifier, ObjectUtils.emptyMap());
            return "ok".equalsIgnoreCase((String) result.get("result"));
        } catch (Exception e) {
            log.warn("Không thể xóa tệp trên Cloudinary: {}", fileIdentifier, e);
            return false;
        }
    }

    @Override
    public String getFileUrl(String fileName, String fileType) {
        if (!StringUtils.hasText(fileName)) return "";
        if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
            return fileName;
        }
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
}

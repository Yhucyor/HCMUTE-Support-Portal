package com.school.counseling.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface IStorageService {

    StorageResult uploadFile(MultipartFile file, String folder);

    boolean deleteFile(String fileIdentifier);

    String getFileUrl(String fileName, String fileType);

    record StorageResult(
            String originalFileName,
            String storedFileName,
            String publicUrl,
            String fileType, // PDF, IMAGE, DOCX, XLSX, MP4
            long fileSizeBytes,
            boolean isSuccess,
            String errorMessage
    ) {
        public static StorageResult success(String originalFileName, String storedFileName, String publicUrl, String fileType, long fileSizeBytes) {
            return new StorageResult(originalFileName, storedFileName, publicUrl, fileType, fileSizeBytes, true, null);
        }

        public static StorageResult failure(String originalFileName, String errorMessage) {
            return new StorageResult(originalFileName, null, null, null, 0, false, errorMessage);
        }
    }
}

package com.school.counseling.module.ticket.controller;

import com.school.counseling.common.storage.IStorageService;
import com.school.counseling.common.util.Constant;
import com.school.counseling.module.chat.entity.Attachment;
import com.school.counseling.module.chat.repository.AttachmentRepository;
import com.school.counseling.module.feed.entity.PostAttachment;
import com.school.counseling.module.feed.repository.PostAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Controller phục vụ xem trực tuyến (inline view) và tải về (download) tệp đính kèm.
 * Hỗ trợ đa định dạng: Hình ảnh (JPG, PNG, WEBP, GIF), Tài liệu (PDF, DOCX, XLSX, TXT, MD, JSON), Tệp nén (ZIP, RAR).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class FileAttachmentController {

    private final AttachmentRepository attachmentRepository;
    private final Optional<PostAttachmentRepository> postAttachmentRepository;
    private final Tika tika = new Tika();

    private static final String[] SEARCH_DIRS = {
            Constant.DIR_IMAGES,
            Constant.DIR_DOCUMENTS,
            Constant.DIR_VIDEOS,
            Constant.DIR,
            "D:/portal/upload/images",
            "D:/portal/upload/documents",
            "D:/portal/upload/videos",
            "D:/portal/upload"
    };

    /**
     * Xem trực tuyến tệp đính kèm theo ID (dùng cho <img>, iframe PDF, preview Word/Excel/Markdown).
     */
    @GetMapping("/files/view/{id}")
    public ResponseEntity<Resource> viewAttachment(@PathVariable("id") Long id) {
        return serveAttachment(id, false);
    }

    /**
     * Tải tệp đính kèm về máy tính theo ID.
     */
    @GetMapping("/files/download/{id}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("id") Long id) {
        return serveAttachment(id, true);
    }

    /**
     * Endpoint tương thích legacy cho ảnh: /image?fname=...
     */
    @GetMapping("/image")
    public ResponseEntity<Resource> serveLegacyImage(@RequestParam("fname") String fname) {
        return serveRawFile(fname, false);
    }

    /**
     * Endpoint tương thích legacy cho tài liệu: /documents/{fileName}
     */
    @GetMapping("/documents/{fileName:.+}")
    public ResponseEntity<Resource> serveLegacyDocument(@PathVariable("fileName") String fileName) {
        return serveRawFile(fileName, false);
    }

    /**
     * Endpoint tương thích legacy cho video: /videos/{fileName}
     */
    @GetMapping("/videos/{fileName:.+}")
    public ResponseEntity<Resource> serveLegacyVideo(@PathVariable("fileName") String fileName) {
        return serveRawFile(fileName, false);
    }

    private ResponseEntity<Resource> serveAttachment(Long id, boolean isDownload) {
        // 1. Tìm trong bảng attachments của Ticket
        Optional<Attachment> attOpt = attachmentRepository.findById(id);
        String originalFileName = null;
        String fileUrl = null;

        if (attOpt.isPresent()) {
            Attachment att = attOpt.get();
            originalFileName = att.getFileName();
            fileUrl = att.getFileUrl();
        } else if (postAttachmentRepository.isPresent()) {
            // 2. Tìm trong bảng post_attachments của Bảng tin / Diễn đàn
            Optional<PostAttachment> postAttOpt = postAttachmentRepository.get().findById(id);
            if (postAttOpt.isPresent()) {
                PostAttachment pa = postAttOpt.get();
                originalFileName = pa.getFileName();
                fileUrl = pa.getFileUrl();
            }
        }

        if (fileUrl == null) {
            log.warn("Không tìm thấy tệp đính kèm với ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Nếu là link Cloudinary hoặc URL ngoài -> Redirect thẳng
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(fileUrl)).build();
        }

        // Trích xuất storedFileName từ fileUrl (ví dụ: /image?fname=abc.jpg hoặc /documents/abc.docx)
        String storedFileName = extractStoredFileName(fileUrl);
        if (!StringUtils.hasText(originalFileName)) {
            originalFileName = storedFileName;
        }

        File fileOnDisk = findFileOnDisk(storedFileName);
        if (fileOnDisk == null || !fileOnDisk.exists()) {
            File directFile = new File(fileUrl);
            if (directFile.exists() && directFile.isFile() && isSafeDocumentOrMedia(directFile)) {
                fileOnDisk = directFile;
            }
        }
        if (fileOnDisk == null || !fileOnDisk.exists()) {
            log.warn("Tệp không tồn tại trên ổ đĩa: {} (từ URL: {})", storedFileName, fileUrl);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return buildResponseEntity(fileOnDisk, originalFileName, isDownload);
    }

    private ResponseEntity<Resource> serveRawFile(String rawFileName, boolean isDownload) {
        if (!StringUtils.hasText(rawFileName)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        File fileOnDisk = findFileOnDisk(rawFileName);
        if (fileOnDisk == null || !fileOnDisk.exists()) {
            log.warn("Không tìm thấy tệp raw trên ổ đĩa: {}", rawFileName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return buildResponseEntity(fileOnDisk, rawFileName, isDownload);
    }

    private String extractStoredFileName(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) return "";
        if (fileUrl.contains("fname=")) {
            return fileUrl.substring(fileUrl.indexOf("fname=") + 6);
        }
        int lastSlash = fileUrl.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < fileUrl.length() - 1) {
            return fileUrl.substring(lastSlash + 1);
        }
        return fileUrl;
    }

    private File findFileOnDisk(String storedFileName) {
        if (!StringUtils.hasText(storedFileName)) return null;

        for (String dirPath : SEARCH_DIRS) {
            File candidate = new File(dirPath, storedFileName);
            if (candidate.exists() && candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    private ResponseEntity<Resource> buildResponseEntity(File file, String originalName, boolean isDownload) {
        try {
            Resource resource = new FileSystemResource(file);
            String mimeType = determineMimeType(file, originalName);

            String encodedFileName = URLEncoder.encode(originalName, StandardCharsets.UTF_8.toString()).replace("+", "%20");
            String dispositionType = isDownload ? "attachment" : "inline";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .contentLength(file.length())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            dispositionType + "; filename=\"" + originalName.replace("\"", "") + "\"; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                    .body(resource);
        } catch (Exception e) {
            log.error("Lỗi khi phục vụ tệp: {}", file.getAbsolutePath(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineMimeType(File file, String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".csv")) return "text/csv; charset=UTF-8";
        if (lower.endsWith(".md")) return "text/markdown; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".txt") || lower.endsWith(".log")) return "text/plain; charset=UTF-8";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".rar")) return "application/x-rar-compressed";
        if (lower.endsWith(".7z")) return "application/x-7z-compressed";
        if (lower.endsWith(".mp4")) return "video/mp4";

        try {
            return tika.detect(file);
        } catch (Exception ignored) {
            return "application/octet-stream";
        }
    }

    private boolean isSafeDocumentOrMedia(File candidate) {
        if (candidate == null || !candidate.exists() || !candidate.isFile()) {
            return false;
        }
        try {
            String path = candidate.getCanonicalPath().toLowerCase();
            if (path.contains("\\windows\\") || path.contains("/windows/")
                    || path.contains("\\system32") || path.contains("/system32")
                    || path.contains("\\program files") || path.contains("/program files")
                    || path.startsWith("/etc") || path.startsWith("/bin") || path.startsWith("/sbin")
                    || path.contains(".m2") || path.contains(".git")) {
                return false;
            }

            String name = candidate.getName().toLowerCase();
            return name.endsWith(".pdf") || name.endsWith(".docx") || name.endsWith(".doc")
                    || name.endsWith(".xlsx") || name.endsWith(".xls") || name.endsWith(".pptx")
                    || name.endsWith(".txt") || name.endsWith(".png") || name.endsWith(".jpg")
                    || name.endsWith(".jpeg") || name.endsWith(".webp") || name.endsWith(".mp4")
                    || name.endsWith(".zip") || name.endsWith(".rar");
        } catch (Exception e) {
            return false;
        }
    }
}

package com.school.counseling.module.feed.controller;

import com.school.counseling.common.util.Constant;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.auth.service.DepartmentQueryService;
import com.school.counseling.module.feed.dto.CreatePostRequest;
import com.school.counseling.module.feed.dto.PostResponseDto;
import com.school.counseling.module.feed.entity.PostAttachment;
import com.school.counseling.module.feed.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

@Slf4j
@Controller
@RequestMapping("/feed/official")
@RequiredArgsConstructor
public class OfficialFeedController {

    private final PostService postService;
    private final DepartmentQueryService departmentQueryService;
    private final UserRepository userRepository;

    /**
     * Bảng tin thông báo chính thức công khai (Guest, Sinh viên, Cán bộ đều xem được)
     */
    @GetMapping
    public String listOfficialAnnouncements(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(Math.max(0, page), 9);
        Page<PostResponseDto> posts = postService.getApprovedOfficialPosts(departmentId, query, pageable);
        List<Department> departments = departmentQueryService.getActiveDepartments();

        model.addAttribute("posts", posts);
        model.addAttribute("departments", departments);
        model.addAttribute("selectedDept", departmentId);
        model.addAttribute("query", query);

        return "feed/official-list";
    }

    /**
     * Xem chi tiết thông báo chính thức kèm tài liệu đính kèm và video
     */
    @GetMapping("/{id}")
    public String viewOfficialPost(@PathVariable("id") Long id, Model model) {
        PostResponseDto post = postService.getPostById(id);
        model.addAttribute("post", post);
        return "feed/official-detail";
    }

    /**
     * Giao diện đăng bài thông báo mới dành cho Cán bộ & Quản trị viên
     */
    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String showCreateForm(Model model) {
        List<Department> departments = departmentQueryService.getActiveDepartments();
        Long staffDeptId = SecurityUtils.getCurrentDepartmentId().orElse(null);

        CreatePostRequest request = new CreatePostRequest();
        if (staffDeptId != null) {
            request.setDepartmentId(staffDeptId);
        }

        model.addAttribute("postRequest", request);
        model.addAttribute("departments", departments);
        model.addAttribute("staffDeptId", staffDeptId);

        return "feed/create-official-post";
    }

    /**
     * Tiếp nhận và xử lý đăng thông báo chính thức (Hỗ trợ Video Hybrid và Đa tệp tài liệu)
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String handleCreatePost(
            @Valid @ModelAttribute("postRequest") CreatePostRequest request,
            BindingResult bindingResult,
            @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
            @RequestParam(value = "documentFiles", required = false) List<MultipartFile> documentFiles,
            @RequestParam(value = "attachment", required = false) MultipartFile legacyAttachment,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentQueryService.getActiveDepartments());
            return "feed/create-official-post";
        }

        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        User author = userRepository.findByIdWithRoleAndDepartment(currentUserId).orElseThrow();

        // Chuẩn hóa danh sách tài liệu văn phòng
        List<MultipartFile> docs = (documentFiles != null) ? new java.util.ArrayList<>(documentFiles) : new java.util.ArrayList<>();
        if (legacyAttachment != null && !legacyAttachment.isEmpty()) {
            docs.add(legacyAttachment);
        }

        PostResponseDto created = postService.createOfficialPostWithUpload(request, videoFile, docs, author);

        redirectAttributes.addFlashAttribute("successMessage", "Đã đăng thông báo chính thức thành công!");
        return "redirect:/feed/official/" + created.getId();
    }

    /**
     * Giao diện Chỉnh sửa bài viết thông báo
     */
    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        PostResponseDto post = postService.getPostById(id);
        List<Department> departments = departmentQueryService.getActiveDepartments();

        CreatePostRequest postRequest = CreatePostRequest.builder()
                .title(post.getTitle())
                .content(post.getContent())
                .departmentId(post.getDepartmentId())
                .isPinned(Boolean.TRUE.equals(post.getIsPinned()))
                .videoEmbedUrl(post.getVideoUrl())
                .youtubeUrl(post.getVideoUrl())
                .build();

        model.addAttribute("post", post);
        model.addAttribute("postRequest", postRequest);
        model.addAttribute("departments", departments);

        return "feed/edit-official-post";
    }

    /**
     * Xử lý lưu cập nhật thông báo chính thức
     */
    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String handleEditPost(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("postRequest") CreatePostRequest request,
            BindingResult bindingResult,
            @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
            @RequestParam(value = "documentFiles", required = false) List<MultipartFile> documentFiles,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            PostResponseDto post = postService.getPostById(id);
            model.addAttribute("post", post);
            model.addAttribute("departments", departmentQueryService.getActiveDepartments());
            return "feed/edit-official-post";
        }

        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        User currentUser = userRepository.findByIdWithRoleAndDepartment(currentUserId).orElseThrow();

        postService.updateOfficialPost(id, request, videoFile, documentFiles, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông báo chính thức thành công!");
        return "redirect:/feed/official/" + id;
    }

    /**
     * Cán bộ / Admin xóa thông báo chính thức
     */
    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String handleDeleteOfficialPost(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = SecurityUtils.getCurrentUserId().orElseThrow();
        try {
            postService.deletePost(id, currentUserId, true);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thông báo chính thức thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa thông báo: " + e.getMessage());
        }

        return "redirect:/feed/official";
    }

    /**
     * Admin ghim hoặc bỏ ghim thông báo chính thức lên đầu Bảng tin
     */
    @PostMapping("/toggle-pin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String togglePinOfficialPost(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Referer", required = false) String referer,
            RedirectAttributes redirectAttributes) {
        try {
            boolean isPinned = postService.togglePinPost(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                    isPinned ? "Đã ghim thông báo lên đầu trang thành công!" : "Đã bỏ ghim thông báo thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể thay đổi trạng thái ghim: " + e.getMessage());
        }
        return (referer != null && !referer.isBlank()) ? "redirect:" + referer : "redirect:/feed/official";
    }

    /**
     * Phục vụ xem trực tiếp tệp PDF đính kèm của bài viết (Inline Preview cho <iframe> và toàn màn hình)
     */
    @GetMapping("/attachment/{id}/view")
    public void previewAttachment(@PathVariable("id") Long id, HttpServletRequest request, HttpServletResponse response) {
        serveAttachmentFile(id, "inline", request, response);
    }

    /**
     * Tải về tệp đính kèm của bài viết
     */
    @GetMapping("/attachment/{id}/download")
    public void downloadAttachment(@PathVariable("id") Long id, HttpServletRequest request, HttpServletResponse response) {
        serveAttachmentFile(id, "attachment", request, response);
    }

    private void serveAttachmentFile(Long attachmentId, String dispositionType, HttpServletRequest request, HttpServletResponse response) {
        PostAttachment attachment = postService.getAttachmentById(attachmentId);
        if (attachment == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String fileUrl = attachment.getFileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Nếu là URL online (Cloudinary / S3 / HTTP)
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            try {
                response.sendRedirect(fileUrl);
                return;
            } catch (Exception e) {
                log.error("Redirect attachment error: ", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        // Tệp trên hệ thống tệp cục bộ
        File rawFile = new File(fileUrl);
        String safeName = rawFile.getName();
        String[] allowedDirs = {
            Constant.DIR_DOCUMENTS,
            Constant.DIR,
            Constant.DIR_VIDEOS,
            Constant.DIR_IMAGES
        };
        File file = null;
        for (String dir : allowedDirs) {
            File candidate = new File(dir, safeName);
            try {
                String candidateCanonical = candidate.getCanonicalPath();
                String dirCanonical = new File(dir).getCanonicalPath();
                if (candidateCanonical.startsWith(dirCanonical) && candidate.exists() && candidate.isFile()) {
                    file = candidate;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (file == null && rawFile.exists() && rawFile.isFile()) {
            try {
                String rawCanonical = rawFile.getCanonicalPath();
                for (String dir : allowedDirs) {
                    if (rawCanonical.startsWith(new File(dir).getCanonicalPath())) {
                        file = rawFile;
                        break;
                    }
                }
                // Nếu tệp nằm ngoài thư mục upload mặc định (vd: tài liệu mẫu / thư mục đồ án / tài liệu AI)
                // Cho phép phục vụ nếu là tệp an toàn (không thuộc thư mục hệ thống nhạy cảm và đúng định dạng)
                if (file == null && isSafeDocumentOrMedia(rawFile)) {
                    file = rawFile;
                }
            } catch (Exception ignored) {}
        }

        if (file == null) {
            String safeFileName = attachment.getFileName() != null ? attachment.getFileName() : "official-document.pdf";
            if ("PDF".equalsIgnoreCase(attachment.getFileType()) || safeFileName.toLowerCase().endsWith(".pdf")) {
                log.info("Tệp PDF cục bộ không tồn tại trên Cloud (Render), tạo tài liệu PDF số hóa động cho: {}", safeFileName);
                serveDynamicOfficialPdf(attachment, dispositionType, response);
                return;
            }
            log.warn("Không tìm thấy tệp đính kèm an toàn hoặc vi phạm Path Traversal: {}", fileUrl);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String fileName = attachment.getFileName() != null ? attachment.getFileName() : file.getName();
        String mimeType = resolveMimeType(fileName);

        response.setContentType(mimeType);
        try {
            response.setHeader("Content-Disposition", org.springframework.http.ContentDisposition.builder(dispositionType)
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build()
                    .toString());
        } catch (Exception e) {
            String asciiFallback = fileName.replaceAll("[^\\x00-\\x7F]", "_");
            response.setHeader("Content-Disposition", dispositionType + "; filename=\"" + asciiFallback + "\"");
        }
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentLengthLong(file.length());

        try (InputStream is = new FileInputStream(file)) {
            StreamUtils.copy(is, response.getOutputStream());
        } catch (Exception e) {
            log.warn("Lỗi khi truyền tệp đính kèm: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isSafeDocumentOrMedia(File candidate) {
        if (candidate == null || !candidate.exists() || !candidate.isFile()) {
            return false;
        }
        try {
            String path = candidate.getCanonicalPath().toLowerCase();
            // Ngăn chặn tuyệt đối các thư mục nhạy cảm hệ điều hành
            if (path.contains("\\windows\\") || path.contains("/windows/")
                    || path.contains("\\system32") || path.contains("/system32")
                    || path.contains("\\program files") || path.contains("/program files")
                    || path.startsWith("/etc") || path.startsWith("/bin") || path.startsWith("/sbin")
                    || path.contains(".m2") || path.contains(".git")) {
                return false;
            }

            // Chỉ cho phép định dạng tài liệu, hình ảnh, video học vụ an toàn
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

    private String resolveMimeType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }

    private void serveDynamicOfficialPdf(PostAttachment attachment, String dispositionType, HttpServletResponse response) {
        String fileName = attachment.getFileName() != null ? attachment.getFileName() : "official-document.pdf";
        String postTitle = (attachment.getPost() != null && attachment.getPost().getTitle() != null)
                ? attachment.getPost().getTitle()
                : fileName;
        String deptName = (attachment.getPost() != null && attachment.getPost().getDepartment() != null)
                ? attachment.getPost().getDepartment().getName()
                : "TRƯỜNG ĐẠI HỌC SƯ PHẠM KỸ THUẬT TP.HCM";

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Header Banner
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(50, 785);
                cs.showText("TRUONG DAI HOC SU PHAM KY THUAT TP. HO CHI MINH");
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                cs.newLineAtOffset(50, 770);
                cs.showText("CONG THONG TIN HO TRO SINH VIEN & QUAN TRI HOC VU (QAUTE PORTAL)");
                cs.endText();

                cs.setLineWidth(1.0f);
                cs.moveTo(50, 760);
                cs.lineTo(545, 760);
                cs.stroke();

                // Document Title
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                cs.newLineAtOffset(50, 725);
                String safeTitle = stripNonAscii(postTitle);
                if (safeTitle.length() > 65) safeTitle = safeTitle.substring(0, 62) + "...";
                cs.showText("VAN BAN: " + safeTitle.toUpperCase());
                cs.endText();

                // Department & Filename info
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 9);
                cs.newLineAtOffset(50, 705);
                cs.showText("Don vi ban hanh: " + stripNonAscii(deptName) + " | Tep: " + stripNonAscii(fileName));
                cs.endText();

                // Body content summary
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                cs.newLineAtOffset(50, 660);
                cs.showText("1. Day la van ban so hoa chinh thuc duoc cong bo tren He thong Bang tin HCMUTE.");
                cs.newLineAtOffset(0, -18);
                cs.showText("2. Sinh vien theo doi toan van noi dung quy che / ke hoach va thuc hien dung han.");
                cs.newLineAtOffset(0, -18);
                cs.showText("3. Truong hop can tu van chi tiet, vui long tao Ticket truc tiep tren he thong.");
                cs.endText();

                // Footer
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 8);
                cs.newLineAtOffset(50, 45);
                cs.showText("Xac thuc dien tu boi QAUTE Portal - Dai hoc Su pham Ky thuat TP.HCM (HCMUTE)");
                cs.endText();
            }

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", dispositionType + "; filename=\"" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_") + "\"");
            response.setHeader("X-Frame-Options", "SAMEORIGIN");
            document.save(response.getOutputStream());
        } catch (Exception e) {
            log.error("Loi khi sinh PDF so hoa dong: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String stripNonAscii(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .replace("Đ", "D").replace("đ", "d")
                .replaceAll("[^\\x00-\\x7F]", "");
    }
}

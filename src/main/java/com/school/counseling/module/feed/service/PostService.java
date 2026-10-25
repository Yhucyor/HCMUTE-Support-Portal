package com.school.counseling.module.feed.service;

import com.school.counseling.common.exception.ResourceNotFoundException;
import com.school.counseling.common.storage.IStorageService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.UserStatus;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.feed.dto.CommentDto;
import com.school.counseling.module.feed.dto.CreatePostRequest;
import com.school.counseling.module.feed.dto.PostAttachmentDto;
import com.school.counseling.module.feed.dto.PostReportResponseDto;
import com.school.counseling.module.feed.dto.PostResponseDto;
import com.school.counseling.module.feed.entity.Comment;
import com.school.counseling.module.feed.entity.Post;
import com.school.counseling.module.feed.entity.PostAttachment;
import com.school.counseling.module.feed.entity.PostReport;
import com.school.counseling.module.feed.repository.CommentRepository;
import com.school.counseling.module.feed.repository.LikeRepository;
import com.school.counseling.module.feed.repository.PostAttachmentRepository;
import com.school.counseling.module.feed.repository.PostReportRepository;
import com.school.counseling.module.feed.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SERVICE QUẢN LÝ BÀI VIẾT & KIỂM DUYỆT (POST SERVICE)
 * 
 * Chức năng:
 * 1. Đăng thông báo chính thức (Official Announcements) từ các Khoa/Phòng kèm file văn bản & video nhúng.
 * 2. Đăng bài diễn đàn sinh viên (Student Forum) có bộ lọc từ cấm tự động BannedWordsFilterService.
 * 3. Quản lý bình luận đa cấp (Nested Comments) và báo cáo vi phạm (Moderation).
 */
@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostAttachmentRepository attachmentRepository;
    private final DepartmentRepository departmentRepository;
    private final CommentRepository commentRepository;
    private final PostReportRepository postReportRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final IStorageService storageService;
    private final BannedWordsFilterService bannedWordsFilterService;

    public PostService(
            PostRepository postRepository,
            PostAttachmentRepository attachmentRepository,
            DepartmentRepository departmentRepository,
            CommentRepository commentRepository,
            PostReportRepository postReportRepository,
            UserRepository userRepository,
            LikeRepository likeRepository,
            IStorageService storageService,
            BannedWordsFilterService bannedWordsFilterService) {
        this.postRepository = postRepository;
        this.attachmentRepository = attachmentRepository;
        this.departmentRepository = departmentRepository;
        this.commentRepository = commentRepository;
        this.postReportRepository = postReportRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.storageService = storageService;
        this.bannedWordsFilterService = bannedWordsFilterService;
    }

    /**
     * Cán bộ đăng thông báo chính thức (qua REST API JSON)
     * tạo bài đăng qua posttype = bảng tin chính thức cho staff 
     */
    @Transactional
    public PostResponseDto createOfficialPost(CreatePostRequest request, User author) {
        return createOfficialPostWithUpload(request, null, Collections.emptyList(), author);
    }

    /**
     * Cán bộ đăng thông báo chính thức kèm upload tệp đơn (tương thích ngược)
     */
    @Transactional
    public PostResponseDto createOfficialPostWithUpload(CreatePostRequest request, MultipartFile file, User author) {
        List<MultipartFile> docs = (file != null && !file.isEmpty()) ? List.of(file) : Collections.emptyList();
        return createOfficialPostWithUpload(request, null, docs, author);
    }

    /**
     * Cán bộ đăng thông báo chính thức hỗ trợ Video Hybrid (Tải file hoặc Nhúng YouTube/Drive)
     * và đính kèm tối đa 5 tài liệu văn phòng
     */
    @Transactional
    public PostResponseDto createOfficialPostWithUpload(
            CreatePostRequest request,
            MultipartFile videoFile,
            List<MultipartFile> documentFiles,
            User author) {

        // 1. Xử lý Video thông báo
        if (videoFile != null && !videoFile.isEmpty()) {
            IStorageService.StorageResult videoResult = storageService.uploadFile(videoFile, "videos");
            if (videoResult.isSuccess()) {
                request.setVideoUrl(videoResult.publicUrl());
            } else {
                log.warn("Tải video lên thất bại: {}", videoResult.errorMessage());
            }
        } else if (request.getVideoEmbedUrl() != null && !request.getVideoEmbedUrl().isBlank()) {
            request.setVideoUrl(request.getVideoEmbedUrl().trim());
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        } else if (author != null && author.getDepartment() != null) {
            department = author.getDepartment();
        }

        String initialVideoStatus = (request.getVideoUrl() != null && !request.getVideoUrl().isBlank()) ? "COMPLETED" : "NONE";

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .postType("OFFICIAL_ANNOUNCEMENT")
                .department(department)
                .author(author)
                .status("APPROVED") // Thông báo từ cán bộ tự động duyệt
                .approvedBy(author)
                .approvedAt(LocalDateTime.now())
                .isPinned(request.isPinned())
                .videoUrl(request.getVideoUrl())
                .videoStatus(initialVideoStatus)
                .viewCount(0)
                .likeCount(0)
                .build();

        Post savedPost = postRepository.save(post);

        // 2. Xử lý lưu các tài liệu văn bản đính kèm
        if (documentFiles != null && !documentFiles.isEmpty()) {
            int count = 0;
            for (MultipartFile docFile : documentFiles) {
                if (docFile != null && !docFile.isEmpty() && count < 5) {
                    IStorageService.StorageResult uploadResult = storageService.uploadFile(docFile, "documents");
                    if (uploadResult.isSuccess()) {
                        PostAttachment docAttachment = PostAttachment.builder()
                                .post(savedPost)
                                .fileName(uploadResult.originalFileName())
                                .fileUrl(uploadResult.publicUrl())
                                .fileType(uploadResult.fileType() != null ? uploadResult.fileType().toUpperCase() : "DOCX")
                                .fileSize(uploadResult.fileSizeBytes())
                                .sourceType("DIRECT_UPLOAD")
                                .build();
                        attachmentRepository.save(docAttachment);
                        savedPost.addAttachment(docAttachment);
                        count++;
                    }
                }
            }
        }

        return mapToDto(savedPost);
    }

    /**
     * Cập nhật thông báo chính thức (Edit Official Post)
     */
    @Transactional
    public PostResponseDto updateOfficialPost(
            Long postId,
            CreatePostRequest request,
            MultipartFile videoFile,
            List<MultipartFile> documentFiles,
            User currentUser) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết", "id", postId));

        boolean isAdmin = currentUser != null && currentUser.getRole() != null && "ROLE_ADMIN".equalsIgnoreCase(currentUser.getRole().getName());
        boolean isAuthor = currentUser != null && post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId());
        if (!isAdmin && !isAuthor) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền chỉnh sửa bài viết này.");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (request.getIsPinned() != null) {
            post.setIsPinned(request.isPinned());
        }

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
            if (department != null) {
                post.setDepartment(department);
            }
        }

        if (videoFile != null && !videoFile.isEmpty()) {
            IStorageService.StorageResult videoResult = storageService.uploadFile(videoFile, "videos");
            if (videoResult.isSuccess()) {
                post.setVideoUrl(videoResult.publicUrl());
                post.setVideoStatus("COMPLETED");
            }
        } else if (request.getVideoEmbedUrl() != null && !request.getVideoEmbedUrl().isBlank()) {
            post.setVideoUrl(request.getVideoEmbedUrl().trim());
            post.setVideoStatus("COMPLETED");
        }

        if (documentFiles != null && !documentFiles.isEmpty()) {
            for (MultipartFile docFile : documentFiles) {
                if (docFile != null && !docFile.isEmpty()) {
                    IStorageService.StorageResult uploadResult = storageService.uploadFile(docFile, "documents");
                    if (uploadResult.isSuccess()) {
                        PostAttachment docAttachment = PostAttachment.builder()
                                .post(post)
                                .fileName(uploadResult.originalFileName())
                                .fileUrl(uploadResult.publicUrl())
                                .fileType(uploadResult.fileType() != null ? uploadResult.fileType().toUpperCase() : "DOCX")
                                .fileSize(uploadResult.fileSizeBytes())
                                .sourceType("DIRECT_UPLOAD")
                                .build();
                        attachmentRepository.save(docAttachment);
                        post.addAttachment(docAttachment);
                    }
                }
            }
        }

        post = postRepository.save(post);
        log.info("Cập nhật bài viết #{} [{}] thành công", post.getId(), post.getTitle());
        return mapToDto(post);
    }

    /**
     * Sinh viên đăng bài thảo luận trên Diễn đàn (Kiểm tra từ cấm và Khởi tạo trạng thái PENDING_APPROVAL)
     */
    @Transactional
    public Post createForumPost(String title, String content, MultipartFile image, User author) {
        return createForumPost(title, content, null, image, author);
    }

    /**
     * Sinh viên đăng bài thảo luận trên Diễn đàn kèm chỉ định Khoa/Phòng
     */
    @Transactional
    public Post createForumPost(String title, String content, Long departmentId, MultipartFile image, User author) {
        // Tự động kiểm tra từ cấm qua BannedWordsFilterService
        bannedWordsFilterService.validateContent(title + " " + content);

        boolean isStaffOrAdmin = author != null && author.getRole() != null &&
                ("ROLE_STAFF".equalsIgnoreCase(author.getRole().getName()) || "ROLE_ADMIN".equalsIgnoreCase(author.getRole().getName()));
        String initialStatus = isStaffOrAdmin ? "APPROVED" : "PENDING_APPROVAL";

        Department dept = null;
        if (departmentId != null) {
            dept = departmentRepository.findById(departmentId).orElse(null);
        }
        if (dept == null && author != null) {
            dept = author.getDepartment();
        }

        Post.PostBuilder postBuilder = Post.builder()
                .title(title)
                .content(content)
                .postType("STUDENT_FORUM")
                .status(initialStatus)
                .author(author)
                .department(dept);

        if (isStaffOrAdmin) {
            postBuilder.approvedBy(author).approvedAt(LocalDateTime.now());
        }

        Post savedPost = postRepository.save(postBuilder.build());

        if (image != null && !image.isEmpty()) {
            String folder = "images";
            String contentType = image.getContentType();
            if (contentType != null && (contentType.contains("pdf") || contentType.contains("word") || contentType.contains("officedocument") || contentType.contains("zip"))) {
                folder = "documents";
            }
            IStorageService.StorageResult uploadResult = storageService.uploadFile(image, folder);
            if (uploadResult.isSuccess()) {
                PostAttachment imageAtt = PostAttachment.builder()
                        .post(savedPost)
                        .fileName(uploadResult.originalFileName())
                        .fileUrl(uploadResult.publicUrl())
                        .fileType(uploadResult.fileType())
                        .fileSize(uploadResult.fileSizeBytes())
                        .sourceType("DIRECT_UPLOAD")
                        .build();
                attachmentRepository.save(imageAtt);
                savedPost.addAttachment(imageAtt);
            }
        }

        log.info("Sinh viên {} đã gửi bài thảo luận ID: {} (Trạng thái: {})", author != null ? author.getUsername() : "anonymous", savedPost.getId(), initialStatus);
        return savedPost;
    }


    /**
     * Sinh viên đăng bài thảo luận trả về DTO cho AJAX Fetch API
     */
    @Transactional
    public PostResponseDto createForumPostDto(String title, String content, MultipartFile image, User author) {
        return createForumPostDto(title, content, null, image, author);
    }

    @Transactional
    public PostResponseDto createForumPostDto(String title, String content, Long departmentId, MultipartFile image, User author) {
        Post savedPost = createForumPost(title, content, departmentId, image, author);
        return mapToDto(savedPost);
    }

    /**
     * Phê duyệt bài viết (Cán bộ / Admin)
     */
    @Transactional
    public void approvePost(Long postId, User approver) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết ID: " + postId));

        post.setStatus("APPROVED");
        post.setApprovedBy(approver);
        post.setApprovedAt(LocalDateTime.now());
        postRepository.save(post);
        log.info("Cán bộ {} đã phê duyệt bài viết ID: {}", approver.getUsername(), postId);
    }

    /**
     * Từ chối bài viết (Cán bộ / Admin)
     */
    @Transactional
    public void rejectPost(Long postId, String reason, User approver) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết ID: " + postId));

        post.setStatus("REJECTED");
        post.setRejectionReason(reason);
        post.setApprovedBy(approver);
        post.setApprovedAt(LocalDateTime.now());
        postRepository.save(post);
        log.info("Cán bộ {} đã từ chối bài viết ID: {} với lý do: {}", approver.getUsername(), postId, reason);
    }

    /**
     * Thêm bình luận cấp 1 vào bài viết
     */
    @Transactional
    public Comment addComment(Long postId, String content, User author) {
        return addComment(postId, content, null, author);
    }

    /**
     * Thêm bình luận vào bài viết (Hỗ trợ trả lời lồng nhau - Nested Reply & kiểm tra từ cấm)
     */
    @Transactional
    public Comment addComment(Long postId, String content, Long parentId, User author) {
        // Tự động kiểm tra từ cấm trong bình luận
        bannedWordsFilterService.validateContent(content);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết ID: " + postId));

        Comment parent = null;
        if (parentId != null) {
            parent = commentRepository.findById(parentId).orElse(null);
        }

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .parent(parent)
                .content(content.trim())
                .likeCount(0)
                .build();

        return commentRepository.save(comment);
    }

    @Transactional
    public CommentDto addCommentDto(Long postId, String content, Long parentId, User author) {
        Comment comment = addComment(postId, content, parentId, author);
        return mapCommentToDto(comment);
    }

    /**
     * Lấy danh sách bình luận kèm tác giả
     */
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdWithAuthor(postId);
    }

    /**
     * Lấy cây bình luận phân cấp (Nested Comments) cho bài viết
     */
    @Transactional(readOnly = true)
    public List<CommentDto> getPostCommentsDto(Long postId) {
        List<Comment> allComments = commentRepository.findByPostIdWithAuthor(postId);
        Map<Long, CommentDto> map = new LinkedHashMap<>();
        List<CommentDto> roots = new ArrayList<>();

        for (Comment cmt : allComments) {
            CommentDto dto = mapCommentToDto(cmt);
            map.put(cmt.getId(), dto);
        }

        for (Comment cmt : allComments) {
            CommentDto dto = map.get(cmt.getId());
            if (cmt.getParent() != null && map.containsKey(cmt.getParent().getId())) {
                map.get(cmt.getParent().getId()).getReplies().add(dto);
            } else {
                roots.add(dto);
            }
        }

        return roots;
    }

    /**
     * Báo cáo bài viết vi phạm
     */
    @Transactional
    public PostReport reportPost(Long postId, String reason, String details, User reporter) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết ID: " + postId));

        PostReport report = PostReport.builder()
                .post(post)
                .reporter(reporter)
                .reason(reason)
                .details(details)
                .status("PENDING")
                .build();

        return postReportRepository.save(report);
    }

    /**
     * Xử lý báo cáo vi phạm
     */
    @Transactional
    public void resolveReport(Long reportId, String action, User staff) {
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo ID: " + reportId));

        Post post = report.getPost();
        if ("HIDE_POST".equalsIgnoreCase(action)) {
            if (post != null) {
                post.setStatus("HIDDEN");
                postRepository.save(post);
            }
        } else if ("LOCK_USER".equalsIgnoreCase(action)) {
            if (post != null) {
                post.setStatus("HIDDEN");
                postRepository.save(post);
                User author = post.getAuthor();
                if (author != null) {
                    author.setStatus(UserStatus.LOCKED);
                    userRepository.save(author);
                    log.warn("Tài khoản {} đã bị khóa do vi phạm nội dung nghiêm trọng", author.getUsername());
                }
            }
        }

        report.setStatus("RESOLVED");
        report.setActionTaken(action);
        report.setResolvedBy(staff);
        report.setResolvedAt(LocalDateTime.now());
        postReportRepository.save(report);
    }

    /**
     * Lấy chi tiết bài viết kèm danh sách tệp đính kèm và video (Tự động tăng viewCount)
     */
    @Transactional
    public PostResponseDto getPostById(Long id) {
        Post post = postRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết ID: " + id));

        int currentViews = (post.getViewCount() == null) ? 0 : post.getViewCount();
        post.setViewCount(currentViews + 1);
        postRepository.save(post);

        List<PostAttachment> attachments = attachmentRepository.findByPostIdAndIsDeletedFalse(id);
        return mapToDto(post, attachments);
    }

    /**
     * Lấy chi tiết Post Entity trực tiếp
     */
    @Transactional(readOnly = true)
    public Post getPostEntity(Long id) {
        return postRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết ID: " + id));
    }

    /**
     * Lấy danh sách thông báo chính thức đã phê duyệt (phân trang cơ bản)
     */
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getApprovedOfficialPosts(Pageable pageable) {
        return getApprovedOfficialPosts(null, null, pageable);
    }

    /**
     * Lấy danh sách thông báo chính thức đã phê duyệt (hỗ trợ lọc theo Khoa/Phòng và tìm kiếm)
     */
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getApprovedOfficialPosts(Long departmentId, String query, Pageable pageable) {
        Page<Post> posts;
        if (query != null && !query.trim().isEmpty()) {
            posts = postRepository.searchApprovedOfficialPosts(query.trim(), pageable);
        } else if (departmentId != null) {
            posts = postRepository.findApprovedOfficialPostsByDepartment(departmentId, pageable);
        } else {
            posts = postRepository.findApprovedPostsByType("OFFICIAL_ANNOUNCEMENT", pageable);
        }
        return posts.map(this::mapToDto);
    }

    /**
     * Tìm kiếm và lọc bài viết trên Diễn đàn sinh viên theo từ khóa và Khoa (DTO chuẩn hóa OSIV=false)
     */
    @Transactional(readOnly = true)
    public Page<PostResponseDto> searchApprovedForumPostsDto(String query, Long departmentId, Long currentUserId, Pageable pageable) {
        Page<Post> posts = postRepository.searchApprovedForumPosts(
                (query != null && !query.trim().isEmpty()) ? query.trim() : null,
                departmentId,
                pageable);

        Set<Long> userLikedPostIds = Collections.emptySet();
        if (currentUserId != null && posts.hasContent()) {
            List<Long> postIds = posts.getContent().stream().map(Post::getId).collect(Collectors.toList());
            userLikedPostIds = likeRepository.findByUserIdAndTargetTypeAndTargetIdIn(currentUserId, "POST", postIds)
                    .stream().map(com.school.counseling.module.feed.entity.Like::getTargetId).collect(Collectors.toSet());
        }

        final Set<Long> likedIds = userLikedPostIds;
        return posts.map(p -> {
            List<PostAttachment> attachments = attachmentRepository.findByPostIdAndIsDeletedFalse(p.getId());
            PostResponseDto dto = mapToDto(p, attachments);
            dto.setUserLiked(likedIds.contains(p.getId()));
            List<Comment> postComments = commentRepository.findByPostIdWithAuthor(p.getId());
            if (postComments != null && !postComments.isEmpty()) {
                dto.setComments(postComments.stream().map(this::mapCommentToDto).collect(Collectors.toList()));
            } else {
                dto.setComments(new ArrayList<>());
            }
            return dto;
        });
    }

    /**
     * Lấy danh sách bài viết của chính tác giả (DTO chuẩn hóa OSIV=false)
     */
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getMyPostsDto(Long authorId, Pageable pageable) {
        Page<Post> posts = postRepository.findPostsByAuthorId(authorId, pageable);

        Set<Long> userLikedPostIds = Collections.emptySet();
        if (authorId != null && posts.hasContent()) {
            List<Long> postIds = posts.getContent().stream().map(Post::getId).collect(Collectors.toList());
            userLikedPostIds = likeRepository.findByUserIdAndTargetTypeAndTargetIdIn(authorId, "POST", postIds)
                    .stream().map(com.school.counseling.module.feed.entity.Like::getTargetId).collect(Collectors.toSet());
        }

        final Set<Long> likedIds = userLikedPostIds;
        return posts.map(p -> {
            List<PostAttachment> attachments = attachmentRepository.findByPostIdAndIsDeletedFalse(p.getId());
            PostResponseDto dto = mapToDto(p, attachments);
            dto.setUserLiked(likedIds.contains(p.getId()));
            List<Comment> postComments = commentRepository.findByPostIdWithAuthor(p.getId());
            if (postComments != null && !postComments.isEmpty()) {
                dto.setComments(postComments.stream().map(this::mapCommentToDto).collect(Collectors.toList()));
            } else {
                dto.setComments(new ArrayList<>());
            }
            return dto;
        });
    }

    /**
     * Xóa mềm bài viết của tác giả (hoặc Staff/Admin)
     */
    @Transactional
    public void deletePost(Long postId, Long currentUserId, boolean isStaffOrAdmin) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết ID: " + postId));

        if (!isStaffOrAdmin && (post.getAuthor() == null || !post.getAuthor().getId().equals(currentUserId))) {
            throw new SecurityException("Bạn không có quyền xóa bài viết này.");
        }

        post.setIsDeleted(true);
        postRepository.save(post);
        log.info("Người dùng ID {} đã xóa bài viết ID {}", currentUserId, postId);
    }

    /**
     * Lấy danh sách bài viết đang chờ kiểm duyệt (Trả về DTO tuân thủ OSIV = false)
     */
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPendingPostsDto(Pageable pageable) {
        return postRepository.findPendingModerationPosts(pageable).map(this::mapToDto);
    }

    /**
     * Lấy danh sách báo cáo vi phạm (Trả về DTO tuân thủ OSIV = false)
     */
    @Transactional(readOnly = true)
    public Page<PostReportResponseDto> getReportsDto(String status, Pageable pageable) {
        return postReportRepository.findReportsByStatus(status, pageable).map(this::mapReportToDto);
    }

    /**
     * Khôi phục / Bỏ ẩn bài viết bị báo cáo nhầm (Cán bộ / Admin)
     */
    @Transactional
    public void unhidePost(Long reportId, User staff) {
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo ID: " + reportId));

        Post post = report.getPost();
        if (post != null) {
            post.setStatus("APPROVED");
            postRepository.save(post);
            log.info("Cán bộ {} đã khôi phục bài viết ID: {}", staff.getUsername(), post.getId());
        }

        report.setActionTaken("RESTORED");
        report.setResolvedBy(staff);
        report.setResolvedAt(LocalDateTime.now());
        postReportRepository.save(report);
    }

    public long countPendingPosts() {
        return postRepository.countByStatusAndIsDeletedFalse("PENDING_APPROVAL");
    }

    public long countPendingReports() {
        return postReportRepository.countByStatus("PENDING");
    }

    public long countResolvedReports() {
        return postReportRepository.countByStatus("RESOLVED");
    }

    /**
     * Ghim / Bỏ ghim bài viết (Pin / Unpin)
     */
    @Transactional
    public boolean togglePinPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết id=" + postId));
        boolean currentPin = Boolean.TRUE.equals(post.getIsPinned());
        post.setIsPinned(!currentPin);
        postRepository.save(post);
        log.info("Đã cập nhật trạng thái ghim bài viết id={}: {} -> {}", postId, currentPin, !currentPin);
        return !currentPin;
    }

    // ─── PRIVATE MAPPERS ────────────────────────────────────────────────────

    private PostReportResponseDto mapReportToDto(PostReport report) {
        PostReportResponseDto.PostSummary postSummary = null;
        if (report.getPost() != null) {
            Post p = report.getPost();
            PostReportResponseDto.AuthorSummary authorSummary = null;
            if (p.getAuthor() != null) {
                authorSummary = new PostReportResponseDto.AuthorSummary(
                        p.getAuthor().getId(),
                        p.getAuthor().getFullName(),
                        p.getAuthor().getEmail()
                );
            }
            postSummary = new PostReportResponseDto.PostSummary(
                    p.getId(),
                    p.getTitle(),
                    p.getContent(),
                    p.getStatus(),
                    authorSummary
            );
        }

        PostReportResponseDto.ReporterSummary reporterSummary = null;
        if (report.getReporter() != null) {
            reporterSummary = new PostReportResponseDto.ReporterSummary(
                    report.getReporter().getId(),
                    report.getReporter().getFullName(),
                    report.getReporter().getEmail()
            );
        }

        String resolvedByName = (report.getResolvedBy() != null) ? report.getResolvedBy().getFullName() : null;

        return PostReportResponseDto.builder()
                .id(report.getId())
                .post(postSummary)
                .reporter(reporterSummary)
                .reason(report.getReason())
                .details(report.getDetails())
                .status(report.getStatus())
                .actionTaken(report.getActionTaken())
                .resolvedByName(resolvedByName)
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }

    private PostResponseDto mapToDto(Post post) {
        return mapToDto(post, null);
    }

    private PostResponseDto mapToDto(Post post, List<PostAttachment> explicitAttachments) {
        List<PostAttachmentDto> attachmentDtos = null;
        List<PostAttachment> attachmentsToMap = (explicitAttachments != null) ? explicitAttachments : post.getAttachments();
        if (attachmentsToMap != null) {
            attachmentDtos = attachmentsToMap.stream()
                    .map(att -> PostAttachmentDto.builder()
                            .id(att.getId())
                            .fileName(att.getFileName())
                            .fileUrl(att.getFileUrl())
                            .fileType(att.getFileType())
                            .fileSize(att.getFileSize())
                            .sourceType(att.getSourceType())
                            .createdAt(att.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        }

        String authorRole = "STUDENT";
        String authorAvatar = null;
        String authorUsername = null;
        String authorEmail = null;
        if (post.getAuthor() != null) {
            authorUsername = post.getAuthor().getUsername();
            authorAvatar = post.getAuthor().getAvatarUrl();
            authorEmail = post.getAuthor().getEmail();
            if (post.getAuthor().getRole() != null) {
                authorRole = post.getAuthor().getRole().getName().replace("ROLE_", "");
            }
        }

        long commentCount = commentRepository.countByPostIdAndIsDeletedFalse(post.getId());

        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .postType(post.getPostType())
                .status(post.getStatus())
                .videoStatus(post.getVideoStatus())
                .videoUrl(post.getVideoUrl())
                .isPinned(post.getIsPinned())
                .viewCount(post.getViewCount() != null ? post.getViewCount() : 0)
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0)
                .commentCount((int) commentCount)
                .rejectionReason(post.getRejectionReason())
                .departmentId(post.getDepartment() != null ? post.getDepartment().getId() : null)
                .departmentName(post.getDepartment() != null ? post.getDepartment().getName() : null)
                .authorId(post.getAuthor() != null ? post.getAuthor().getId() : null)
                .authorName(post.getAuthor() != null ? post.getAuthor().getFullName() : null)
                .authorEmail(authorEmail)
                .authorUsername(authorUsername)
                .authorRole(authorRole)
                .authorAvatar(authorAvatar)
                .attachments(attachmentDtos)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public CommentDto mapCommentToDto(Comment cmt) {
        String roleName = "STUDENT";
        String deptName = null;
        String avatar = null;
        String username = null;
        String fullName = "Thành viên";
        Long authorId = null;

        if (cmt.getAuthor() != null) {
            authorId = cmt.getAuthor().getId();
            fullName = cmt.getAuthor().getFullName();
            username = cmt.getAuthor().getUsername();
            avatar = cmt.getAuthor().getAvatarUrl();
            if (cmt.getAuthor().getRole() != null) {
                roleName = cmt.getAuthor().getRole().getName().replace("ROLE_", "");
            }
            if (cmt.getAuthor().getDepartment() != null) {
                deptName = cmt.getAuthor().getDepartment().getName();
            }
        }

        return CommentDto.builder()
                .id(cmt.getId())
                .postId(cmt.getPost() != null ? cmt.getPost().getId() : null)
                .authorId(authorId)
                .authorName(fullName)
                .authorUsername(username)
                .authorRole(roleName)
                .authorDepartment(deptName)
                .authorAvatar(avatar)
                .content(cmt.getContent())
                .parentId(cmt.getParent() != null ? cmt.getParent().getId() : null)
                .likeCount(cmt.getLikeCount() != null ? cmt.getLikeCount() : 0)
                .createdAt(cmt.getCreatedAt())
                .replies(new ArrayList<>())
                .build();
    }
}

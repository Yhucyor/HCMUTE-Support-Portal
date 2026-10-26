package com.school.counseling.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.counseling.module.ai.entity.Faq;
import com.school.counseling.module.ai.repository.FaqRepository;
import com.school.counseling.module.ai.service.SmartFaqMatcherService;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.Role;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.UserStatus;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.RoleRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.feed.entity.Comment;
import com.school.counseling.module.feed.entity.Post;
import com.school.counseling.module.feed.entity.PostAttachment;
import com.school.counseling.module.feed.repository.CommentRepository;
import com.school.counseling.module.feed.repository.PostAttachmentRepository;
import com.school.counseling.module.feed.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tự động khởi tạo dữ liệu mẫu (Roles, Departments, Sample Users, FAQ Dataset, Forum Posts) khi ứng dụng
 * chạy lần đầu
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FaqRepository faqRepository;
    private final SmartFaqMatcherService smartFaqMatcherService;
    private final ObjectMapper objectMapper;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DataInitializer(RoleRepository roleRepository,
                           DepartmentRepository departmentRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           FaqRepository faqRepository,
                           SmartFaqMatcherService smartFaqMatcherService,
                           ObjectMapper objectMapper,
                           PostRepository postRepository,
                           CommentRepository commentRepository,
                           PostAttachmentRepository postAttachmentRepository,
                           org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.faqRepository = faqRepository;
        this.smartFaqMatcherService = smartFaqMatcherService;
        this.objectMapper = objectMapper;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postAttachmentRepository = postAttachmentRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Bắt đầu kiểm tra và khởi tạo dữ liệu mẫu hệ thống...");

            // 0. Dọn dẹp tài khoản rác test (race_staff_1 -> race_staff_10)
            cleanupMockRaceStaffUsers();
            cleanupDuplicateComments();

            // 1. Khởi tạo 3 Vai trò (Roles)
            Role roleStudent = initRole("ROLE_STUDENT", "Sinh viên trường ĐH Sư phạm Kỹ thuật TP.HCM");
            Role roleStaff = initRole("ROLE_STAFF", "Cán bộ / Giảng viên tư vấn các Khoa/Phòng");
            Role roleAdmin = initRole("ROLE_ADMIN", "Quản trị viên toàn quyền hệ thống");

            // 2. Khởi tạo 5 Khoa/Phòng ban đầu mối
            Department deptDoan = initDepartment("Đoàn Thanh niên - Hội Sinh viên", "DOAN_HOI", "A1-102",
                    "doantn@hcmute.edu.vn", "028.3722.1223",
                    "Phong trào sinh viên, tình nguyện, rèn luyện, truyền thông");
            Department deptTuyenSinh = initDepartment("Phòng Tuyển sinh & Truyền thông", "TUYEN_SINH", "A1-101",
                    "tuyensinh@hcmute.edu.vn", "028.3722.5766", "Tư vấn tuyển sinh các hệ, đề án tuyển sinh");
            Department deptDaoTao = initDepartment("Phòng Đào tạo & Công tác Sinh viên", "DAO_TAO", "A1-201",
                    "daotao@hcmute.edu.vn", "028.3896.8641",
                    "Học vụ, điểm số, đăng ký môn học, chứng chỉ ngoại ngữ TOEIC, học bổng");
            Department deptCntt = initDepartment("Khoa Công nghệ Thông tin", "KHOA_CNTT", "E1-402",
                    "cntt@hcmute.edu.vn", "028.3897.2092",
                    "Đồ án tốt nghiệp, học phần chuyên ngành CNTT, thực tập doanh nghiệp");
            Department deptNn = initDepartment("Khoa Ngoại ngữ", "KHOA_NN", "A1-306", "nn@hcmute.edu.vn",
                    "028.3896.1373", "Chuyển điểm chuẩn đầu ra tiếng Anh, kỳ thi ĐGNLTA đầu vào");
            Department deptCkm = initDepartment("Khoa Cơ khí Chế tạo máy", "KHOA_CKM", "A1-401",
                    "ckm@hcmute.edu.vn", "028.3896.8642",
                    "Đồ án tốt nghiệp CKM, Cơ điện tử, Kỹ thuật Công nghiệp");
            Department deptDien = initDepartment("Khoa Điện - Điện tử", "KHOA_DIEN", "B1-201",
                    "feee@hcmute.edu.vn", "028.3896.8643",
                    "Tự động hóa, Viễn thông, IoT, Điện tử công nghiệp");
            Department deptKt = initDepartment("Khoa Kinh tế", "KHOA_KT", "A1-302",
                    "kinhte@hcmute.edu.vn", "028.3896.8644",
                    "Kế toán, Quản trị kinh doanh, Thương mại điện tử, Logistics");

            // 3. Khởi tạo Tài khoản mẫu (Mật khẩu: 123456 hoặc Password123@)
            String defaultPassword = "123456";

            // Tài khoản Quản trị viên
            initUser("admin", "123456", "Quản Trị Viên Hệ Thống", "admin@hcmute.edu.vn", roleAdmin, null);
            initUser("admin_main", "123456", "Admin Quản Trị Cấp Cao", "admin@gmail.com", roleAdmin, null);

            // Tài khoản Cán bộ các đơn vị
            User staffAcademic = initUser("staff_academic", defaultPassword, "Cán bộ Quản lý Đào tạo", "staff.academic@hcmute.edu.vn",
                    roleStaff, deptDaoTao);
            User staffDaotao = initUser("staff_daotao", defaultPassword, "ThS. Phạm Thị Thu Sương", "daotao.staff@hcmute.edu.vn",
                    roleStaff, deptDaoTao);
            User staffTuyensinh = initUser("staff_tuyensinh", defaultPassword, "ThS. Nguyễn Văn Tuyển Sinh", "tuyensinh.staff@hcmute.edu.vn",
                    roleStaff, deptTuyenSinh);
            User staffCntt = initUser("staff_cntt", defaultPassword, "TS. Trần Khoa CNTT", "cntt.staff@hcmute.edu.vn", roleStaff,
                    deptCntt);
            User staffDoan = initUser("staff_doan", defaultPassword, "Đ/c Lê Bí Thư Đoàn", "doantn.staff@hcmute.edu.vn", roleStaff,
                    deptDoan);
            User staffNn = initUser("staff_nn", defaultPassword, "ThS. Giảng Viên Ngoại Ngữ", "nn.staff@hcmute.edu.vn", roleStaff,
                    deptNn);

            // Tài khoản Sinh viên
            initUser("student", defaultPassword, "Nguyễn Văn Sinh Viên Mẫu", "student@student.hcmute.edu.vn", roleStudent, deptCntt);
            initUser("student01", "123456", "Nguyễn Văn Sinh Viên", "student01@hcmute.edu.vn", roleStudent, deptCntt);
            initUser("lecuong", "123456", "Lê Quốc Cường", "lecuong@gmail.com", roleStudent, deptCntt);

            User minhQuan = initUser("minhquan", defaultPassword, "Trần Minh Quân", "minhquan@student.hcmute.edu.vn", roleStudent, deptCntt,
                    "https://lh3.googleusercontent.com/aida-public/AB6AXuAVU9HbZTIOX9rzxeKlQgVSwXNZRAKh1GYcUUU13fLLlr_C27_HVGExFs3bJ3wjKYEUUPeSGyn8hsm4wIagD_gBb6DJZlTYX4oeAADm21fv8gh-f1FQW4IPK1nGNtvINyB2OGWFOtoI1tAQAbSFYiLFbV51m6ohOSpl3KXPceHt6lzdJCmlQyWMaOoYGogQS96vAXmP68P998VH-k0qfCCou7DyEICE320G7ZhDIqf1p9VmagWMDAiG");
            User vanDuc = initUser("vanduc", defaultPassword, "Phạm Văn Đức", "vanduc@student.hcmute.edu.vn", roleStudent, deptCntt,
                    "https://lh3.googleusercontent.com/aida-public/AB6AXuBHztsTdGw-81EQZb9gUJyCL3fosvAV_nkGLW0pi8ohIJzi4_Bepm3aucaS-trGuovfZCHddxEtpfThYi0qHBGwMZqib-5htyBi6t9HCj9N4yRt0FCJTT2Zv4ZKGo-Kk4e3BCKy1SVrX0P3QdOuBjDo8jIyv1uI5mY5uPFWEQL7xH8bXZQFvkoIQgkHoTIQEIqVAGe93UBN7PhLZObVfq3zgflNqn71VoqeOKK70kfpF7ikE-AFy4Kb");
            User camTu = initUser("camtu", defaultPassword, "Đặng Thị Cẩm Tú", "camtu@student.hcmute.edu.vn", roleStudent, deptCkm);
            User hoangNam = initUser("hoangnam", defaultPassword, "Nguyễn Hoàng Nam", "hoangnam@student.hcmute.edu.vn", roleStudent, deptCkm);
            User tuanAnh = initUser("tuananh", defaultPassword, "Lê Tuấn Anh", "tuananh@student.hcmute.edu.vn", roleStudent, deptDien);
            User phuongAnh = initUser("phuonganh", defaultPassword, "Nguyễn Phương Anh", "phuonganh@student.hcmute.edu.vn", roleStudent, deptKt);
            User quocBao = initUser("quocbao", defaultPassword, "Trần Quốc Bảo", "quocbao@student.hcmute.edu.vn", roleStudent, deptCntt);
            User thanhTung = initUser("thanhtung", defaultPassword, "Bùi Thanh Tùng", "thanhtung@student.hcmute.edu.vn", roleStudent, deptCntt);
            User bichNgan = initUser("bichngan", defaultPassword, "Võ Thị Bích Ngân", "bichngan@student.hcmute.edu.vn", roleStudent, deptKt);
            User minhTri = initUser("minhtri", defaultPassword, "Hoàng Minh Trí", "minhtri@student.hcmute.edu.vn", roleStudent, deptDien);
            User giaHuy = initUser("giahuy", defaultPassword, "Đỗ Gia Huy", "giahuy@student.hcmute.edu.vn", roleStudent, deptCntt);
            User vanHau = initUser("vanhau", defaultPassword, "Phan Văn Hậu", "vanhau@student.hcmute.edu.vn", roleStudent, deptCkm);

            // 4. Khởi tạo / Nạp Kho Tri Thức FAQs từ docs/dataset/faq_dataset_curated.json
            syncFaqsFromDataset(deptDoan, deptTuyenSinh, deptDaoTao, deptCntt, deptNn);
            smartFaqMatcherService.reloadCache();

            // 5. Khởi tạo / Nạp bài viết và bình luận mẫu cho Diễn đàn sinh viên
            seedStudentForumPosts(deptCntt, deptCkm, deptDien, deptKt, deptDaoTao, deptNn, deptDoan,
                    staffAcademic, staffDaotao, staffNn, staffDoan,
                    minhQuan, vanDuc, camTu, hoangNam, tuanAnh, phuongAnh, quocBao, thanhTung, bichNgan, minhTri, giaHuy, vanHau);

            // 6. Khởi tạo / Nạp 4 bài viết thông báo tin tức chính thức tiêu điểm từ trang chủ vào Bảng tin
            seedOfficialAnnouncementsFromHomepage(deptTuyenSinh, deptCntt, deptDaoTao, staffTuyensinh, staffCntt, staffDaotao, staffAcademic);

            // 7. Khởi tạo / Nạp 20 công văn quy chế học vụ trọng tâm chuẩn hóa vào Bảng tin
            seed20OfficialRegulations();

            log.info("Khởi tạo dữ liệu mẫu Auth, FAQ, Diễn đàn & Bảng tin thông báo thành công!");
        } catch (Exception e) {
            log.error("Lỗi trong quá trình khởi tạo dữ liệu mẫu: ", e);
        }
    }

    private void syncFaqsFromDataset(Department deptDoan, Department deptTuyenSinh, Department deptDaoTao, Department deptCntt, Department deptNn) {
        try {
            File file = new File("docs/dataset/faq_dataset_curated.json");
            if (!file.exists()) {
                log.info("Không tìm thấy tệp dataset FAQ tại docs/dataset/faq_dataset_curated.json, bỏ qua nạp FAQ");
                return;
            }

            log.info("Bắt đầu nạp / đồng bộ kho tri thức FAQ từ {}...", file.getPath());
            List<Map<String, Object>> records = objectMapper.readValue(file, new TypeReference<>() {});
            Set<String> existingQuestions = faqRepository.findAll().stream()
                    .map(f -> f.getQuestion().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            List<Faq> entities = new ArrayList<>();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

            for (Map<String, Object> r : records) {
                String title = (String) r.get("title");
                String rawQuestion = (String) r.get("question");
                String directAnswer = (String) r.get("answer");
                List<Map<String, String>> replies = (List<Map<String, String>>) r.get("replies");

                String answer = "Vui lòng liên hệ trực tiếp phòng ban phụ trách để được hướng dẫn chi tiết.";
                if (directAnswer != null && !directAnswer.trim().isEmpty()) {
                    answer = directAnswer.trim();
                } else if (replies != null && !replies.isEmpty()) {
                    String firstContent = replies.get(0).get("content");
                    if (firstContent != null && !firstContent.trim().isEmpty()) {
                        answer = firstContent.trim();
                    }
                }

                String cleanQuestion = (title != null && !title.trim().isEmpty()) ? title.trim() : rawQuestion;
                if (cleanQuestion == null || cleanQuestion.isBlank()) {
                    continue;
                }
                if (cleanQuestion.length() > 490) {
                    cleanQuestion = cleanQuestion.substring(0, 490);
                }

                if (existingQuestions.contains(cleanQuestion.trim().toLowerCase(Locale.ROOT))) {
                    continue;
                }

                LocalDateTime postDate = null;
                Object postDateObj = r.get("post_date");
                if (postDateObj != null) {
                    String postDateStr = postDateObj.toString().trim();
                    if (!postDateStr.isEmpty()) {
                        try {
                            postDate = LocalDateTime.parse(postDateStr, dtf);
                        } catch (Exception ex) {
                            log.debug("Không parse được post_date '{}'", postDateStr);
                        }
                    }
                }

                Department targetDept = classifyDepartment(cleanQuestion, deptDoan, deptTuyenSinh, deptDaoTao, deptCntt, deptNn);
                String category = resolveCategory(cleanQuestion, targetDept);
                String keywords = cleanQuestion.length() > 250 ? cleanQuestion.substring(0, 250) : cleanQuestion;

                int views = 1;
                try {
                    Object viewsObj = r.get("views");
                    if (viewsObj != null) {
                        views = Integer.parseInt(viewsObj.toString().trim());
                    }
                } catch (Exception ignored) {}

                entities.add(Faq.builder()
                        .question(cleanQuestion)
                        .answer(answer)
                        .department(targetDept)
                        .category(category)
                        .keywords(keywords)
                        .viewCount(views)
                        .postDate(postDate)
                        .isActive(true)
                        .build());
                existingQuestions.add(cleanQuestion.trim().toLowerCase(Locale.ROOT));
            }

            if (!entities.isEmpty()) {
                faqRepository.saveAll(entities);
                log.info("Đã đồng bộ bổ sung thành công {} câu hỏi FAQ mới vào Database!", entities.size());
            }
        } catch (Exception e) {
            log.warn("Lỗi khi nạp dữ liệu từ dataset FAQ: {}", e.getMessage(), e);
        }
    }

    private Department classifyDepartment(String text, Department deptDoan, Department deptTuyenSinh, Department deptDaoTao, Department deptCntt, Department deptNn) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("tuyển sinh") || lower.contains("xét tuyển") || lower.contains("học phí") || lower.contains("chỉ tiêu") || lower.contains("thí sinh") || lower.contains("học bạ")) {
            return deptTuyenSinh;
        }
        if (lower.contains("tiếng anh") || lower.contains("toeic") || lower.contains("vstep") || lower.contains("ielts") || lower.contains("đgnlta") || lower.contains("ngoại ngữ")) {
            return deptNn;
        }
        if (lower.contains("công nghệ thông tin") || lower.contains("cntt") || lower.contains("lập trình") || lower.contains("phần mềm") || lower.contains("đồ án tốt nghiệp cntt")) {
            return deptCntt;
        }
        if (lower.contains("đoàn") || lower.contains("hội sinh viên") || lower.contains("tình nguyện") || lower.contains("mùa hè xanh") || lower.contains("rèn luyện") || lower.contains("kết nạp")) {
            return deptDoan;
        }
        return deptDaoTao;
    }

    private String resolveCategory(String text, Department dept) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("học phí")) return "Học phí";
        if (lower.contains("tiếng anh") || lower.contains("toeic")) return "Ngoại ngữ";
        if (lower.contains("đăng ký") || lower.contains("môn học")) return "Đăng ký môn học";
        if (lower.contains("tốt nghiệp")) return "Xét tốt nghiệp";
        if (lower.contains("học bổng")) return "Học bổng";
        return "Quy chế học vụ";
    }

    private Role initRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(name)
                        .description(description)
                        .build()));
    }

    private Department initDepartment(String name, String code, String officeLocation, String contactEmail,
            String contactPhone, String description) {
        return departmentRepository.findByCode(code)
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .name(name)
                        .code(code)
                        .officeLocation(officeLocation)
                        .contactEmail(contactEmail)
                        .contactPhone(contactPhone)
                        .description(description)
                        .isActive(true)
                        .build()));
    }

    private User initUser(String username, String rawPassword, String fullName, String email, Role role,
            Department department) {
        return initUser(username, rawPassword, fullName, email, role, department, null);
    }

    private User initUser(String username, String rawPassword, String fullName, String email, Role role,
            Department department, String customAvatarUrl) {
        return userRepository.findByUsername(username).map(existing -> {
            boolean updated = false;
            if (!passwordEncoder.matches(rawPassword, existing.getPasswordHash())) {
                existing.setPasswordHash(passwordEncoder.encode(rawPassword));
                updated = true;
            }
            if (existing.getStatus() != UserStatus.ACTIVE) {
                existing.setStatus(UserStatus.ACTIVE);
                updated = true;
            }
            if (existing.getRole() == null && role != null) {
                existing.setRole(role);
                updated = true;
            }
            if (existing.getDepartment() == null && department != null) {
                existing.setDepartment(department);
                updated = true;
            }
            return updated ? userRepository.save(existing) : existing;
        }).orElseGet(() -> {
            String avatar = (customAvatarUrl != null && !customAvatarUrl.isBlank())
                    ? customAvatarUrl
                    : "https://ui-avatars.com/api/?name=" + fullName.replace(" ", "+") + "&background=0D8ABC&color=fff";
            User user = User.builder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName(fullName)
                    .email(email)
                    .role(role)
                    .department(department)
                    .status(UserStatus.ACTIVE)
                    .avatarUrl(avatar)
                    .build();
            User saved = userRepository.save(user);
            log.info("Đã tạo tài khoản mẫu: {} | Mật khẩu: {}", username, rawPassword);
            return saved;
        });
    }

    private Post initForumPost(String title, String content, Department department, User author, int viewCount, int likeCount, List<String> imageUrls) {
        if (postRepository.existsByTitle(title)) {
            return postRepository.findFirstByTitleContainingAndIsDeletedFalse(title).orElse(null);
        }

        Post post = Post.builder()
                .title(title)
                .content(content)
                .postType("STUDENT_FORUM")
                .status("APPROVED")
                .department(department)
                .author(author)
                .approvedBy(author)
                .approvedAt(LocalDateTime.now())
                .viewCount(viewCount)
                .likeCount(likeCount)
                .build();

        Post saved = postRepository.save(post);

        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imgUrl : imageUrls) {
                PostAttachment attachment = PostAttachment.builder()
                        .post(saved)
                        .fileName("illustration.jpg")
                        .fileUrl(imgUrl)
                        .fileType("IMAGE")
                        .fileSize(204800L)
                        .sourceType("DIRECT_UPLOAD")
                        .build();
                postAttachmentRepository.save(attachment);
            }
        }
        return saved;
    }

    private void initComment(Post post, User author, String content, int likeCount) {
        if (post == null || author == null) return;
        if (commentRepository.existsByPostIdAndAuthorIdAndContent(post.getId(), author.getId(), content)) {
            return;
        }
        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .content(content)
                .likeCount(likeCount)
                .build();
        commentRepository.save(comment);
    }

    private void seedStudentForumPosts(Department deptCntt, Department deptCkm, Department deptDien, Department deptKt, Department deptDaoTao, Department deptNn, Department deptDoan,
                                      User staffAcademic, User staffDaotao, User staffNn, User staffDoan,
                                      User minhQuan, User vanDuc, User camTu, User hoangNam, User tuanAnh, User phuongAnh, User quocBao, User thanhTung, User bichNgan, User minhTri, User giaHuy, User vanHau) {
        log.info("Bắt đầu nạp dữ liệu bài viết và bình luận mẫu cho Diễn đàn sinh viên...");

        // 1. Bài viết 1: Kinh nghiệm đăng ký học phần HK2 (Trần Minh Quân)
        Post p1 = initForumPost(
                "Kinh nghiệm đăng ký học phần HK2 không bị trùng lịch thi & Mẹo né lớp full",
                "Chào các bạn K24! Đợt đăng ký tín chỉ vừa rồi mình thấy nhiều bạn bị vướng lỗi 'Trùng lịch thi kết thúc học phần' hoặc bị out môn tiên quyết.\n\nMình đã tổng hợp một file Excel mẫu lập trước TKB 3 phương án dự phòng và danh sách các mã môn tương đương. Lưu ý: các môn đại cương như Triết học, Pháp luật đại cương nên đăng ký vào khung giờ sớm để tránh quá tải hệ thống. Chúc các bạn đăng ký suôn sẻ!",
                deptCntt, minhQuan, 412, 128, Collections.emptyList()
        );
        initComment(p1, camTu, "Cảm ơn anh Quân rất nhiều ạ! Kỳ trước em bị trùng lịch thi 2 môn chuyên ngành phải làm đơn hủy khổ sở luôn.", 12);
        initComment(p1, staffAcademic, "Phòng Đào tạo lưu ý các em sinh viên: Hãy theo dõi kỹ thời khóa biểu dự kiến trên trang online.hcmute.edu.vn trước ngày mở cổng để chọn nhóm lớp hợp lý nhé.", 24);

        // 2. Bài viết 2: Kinh nghiệm làm đồ án nhóm & Git (Phạm Văn Đức) - Kèm ảnh thực tế từ bài mẫu cũ
        Post p2 = initForumPost(
                "Kinh nghiệm thực hiện đồ án nhóm & Quản lý Git repository hạn chế merge conflicts",
                "Nhóm mình vừa bảo vệ thành công đồ án Chuyên đề AI & Web với điểm 9.5.\n\nChia sẻ với anh em quy trình Git Flow chuẩn: Phân chia rõ branch theo feature, pull request bắt buộc có ít nhất 1 bạn review code trước khi merge, và commit message theo chuẩn Conventional Commits. Đừng bao giờ push thẳng code chưa compile lên nhánh main nhé!",
                deptCntt, vanDuc, 254, 96,
                List.of(
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuAN6tQBlnUbxJbPNhMm8cKd2FMk-tUvsP3FaGp15bJfs_uxgQ7Y0hN2WkDisg78KoLZ_F8aV9b1aQP1Q81hyDWIcYWNZK-SyPY-dvHL6wdUDqWFGm6AMqeC5CXHOJ3SWrRLKP7Fm5o2TG1fPNgGvogY6lgr6clWEHKpolWlpbCme9YWtlt3zbzlTcZT98S6j6fnycbIwtUUTPystReBtCOdGmSrB50LtNMPERnMzkb2aUHAZIO6TQot",
                        "https://lh3.googleusercontent.com/aida-public/AB6AXuB_cxY7wLPWNvg3YhYAYWdHAi3JbnSIsdvjji7fRrI8aO2c_6QEICkzODYwL_1Ctmf6RCtfW-juedxqHfrqe6N4swtVZVAolqqKNfgCKMqz82cRlAFstP2t8pWQ-M5rqipFq3wc6NPGB0RJ_Q-p1Bf5haS401wG5b8kIZ3ze2pyq88eacV8g7QubWKqgzsEzf-A09L16qTfKGdS7bmdzLQDrkvFixOSeyXxh_3SfsgjbP9gg9TqZ9An"
                )
        );
        initComment(p2, giaHuy, "Cho mình hỏi nhóm bạn dùng GitKraken hay source control có sẵn trong VS Code vậy?", 8);
        initComment(p2, vanDuc, "@Gia Huy: Nhóm mình dùng GitHub Desktop kết hợp git cli terminal cho chuẩn bài nha bạn.", 15);

        // 3. Bài viết 3: Chuẩn đầu ra Ngoại ngữ AVĐR (Đặng Thị Cẩm Tú)
        Post p3 = initForumPost(
                "Kiến nghị mở thêm đợt thi Chuẩn đầu ra Ngoại ngữ (AVĐR) tháng 11/2026 để kịp xét tốt nghiệp tháng 12",
                "Em chào thầy cô và các bạn, hiện tại em đã hoàn thành 150 tín chỉ, chỉ còn thiếu chứng chỉ AVĐR. Theo thông tin từ khoa CKM thì sắp tới trường chưa có kế hoạch mở thi tháng 11.\n\nEm kính mong nhà trường xem xét tạo điều kiện mở thêm đợt thi AVĐR để sinh viên kịp xét tốt nghiệp đợt 4 năm 2026. Trường hợp không mở thi thì nộp chứng chỉ quốc tế TOEIC ngoài trường được xét thế nào ạ?",
                deptNn, camTu, 318, 85, Collections.emptyList()
        );
        initComment(p3, staffNn, "Chào em! Hiện tại Nhà trường tiếp nhận chứng chỉ tiếng Anh quốc tế (TOEIC từ IIG, IELTS...). Em nộp hồ sơ chuyển điểm trực tiếp tại VP Khoa Ngoại ngữ phòng A1-306 trước ngày 15 hàng tháng để kịp xét đợt tháng 12 nhé.", 35);
        initComment(p3, camTu, "Dạ em cảm ơn Thầy/Cô Khoa Ngoại ngữ đã giải đáp rõ ràng ạ!", 6);

        // 4. Bài viết 4: Đồ án tốt nghiệp CKM (Nguyễn Hoàng Nam)
        Post p4 = initForumPost(
                "Chia sẻ bí quyết chuẩn bị và bảo vệ Đồ án tốt nghiệp đạt điểm A Khoa Cơ khí Chế tạo máy",
                "Chào các bạn khóa dưới! Mình vừa hoàn thành bảo vệ Đồ án tốt nghiệp kỳ vừa rồi với đề tài 'Thiết kế hệ thống tay gắp phân loại sản phẩm tự động'.\n\nMột số lưu ý quan trọng: 1) Tính toán sức bền chi tiết rõ ràng trong thuyết minh; 2) Bản vẽ kỹ thuật 2D phải tuân thủ nghiêm ngặt tiêu chuẩn TCVN/ISO; 3) Chuẩn bị slide thuyết trình súc tích trong 10 phút, tập trung vào kết quả mô phỏng và sản phẩm thực nghiệm.",
                deptCkm, hoangNam, 189, 72,
                List.of("https://lh3.googleusercontent.com/aida-public/AB6AXuAN6tQBlnUbxJbPNhMm8cKd2FMk-tUvsP3FaGp15bJfs_uxgQ7Y0hN2WkDisg78KoLZ_F8aV9b1aQP1Q81hyDWIcYWNZK-SyPY-dvHL6wdUDqWFGm6AMqeC5CXHOJ3SWrRLKP7Fm5o2TG1fPNgGvogY6lgr6clWEHKpolWlpbCme9YWtlt3zbzlTcZT98S6j6fnycbIwtUUTPystReBtCOdGmSrB50LtNMPERnMzkb2aUHAZIO6TQot")
        );
        initComment(p4, vanHau, "Bản vẽ chi tiết trục và bánh răng có cần in khổ A0 không anh Nam ơi?", 5);
        initComment(p4, hoangNam, "Có em nhé, in A0 hoặc A1 đóng quyển bản vẽ nộp hội đồng chấm duyệt.", 11);

        // 5. Bài viết 5: Tuyển thành viên NCKH (Lê Tuấn Anh)
        Post p5 = initForumPost(
                "Tuyển thành viên tham gia nhóm Nghiên cứu khoa học sinh viên (NCKH) đề tài IoT & Smart Campus 2026",
                "Chào các bạn, nhóm mình hiện có 2 người đang phát triển đề tài NCKH cấp trường năm học 2026: 'Hệ thống giám sát tiêu thụ năng lượng và chất lượng không khí thông minh tại các giảng đường HCMUTE'.\n\nNhóm cần tìm thêm 1 bạn phụ trách phần cứng vi điều khiển (ESP32/STM32) và 1 bạn làm web dashboard. Quyền lợi: Được cộng điểm rèn luyện, bài báo đăng kỷ yếu và hỗ trợ kinh phí từ phòng NCKH.",
                deptDien, tuanAnh, 210, 64, Collections.emptyList()
        );
        initComment(p5, minhQuan, "Đề tài hay quá bạn ơi, mình bên CNTT phụ trách web/backend Spring Boot được không?", 9);
        initComment(p5, tuanAnh, "Quá tuyệt vời luôn bạn, mình đã gửi tin nhắn kết nối với bạn rồi nhé!", 14);

        // 6. Bài viết 6: Thủ tục cấp bảng điểm & giấy xác nhận (Nguyễn Phương Anh)
        Post p6 = initForumPost(
                "Hướng dẫn chi tiết thủ tục xin cấp bảng điểm tiếng Anh và giấy xác nhận sinh viên tại Phòng Đào tạo A1-201",
                "Dành cho các bạn năm cuối đang chuẩn bị nộp hồ sơ xin việc hoặc học bổng: Thủ tục xin cấp Bảng điểm song ngữ Anh - Việt hiện tại các bạn có thể đăng ký trực tuyến trên cổng online.hcmute.edu.vn, sau đó chọn nhận tại Bộ phận Một cửa A1-201 sau 2 - 3 ngày làm việc. Lệ phí 10.000đ/bản, đóng qua cổng thanh toán VNPay rất tiện lợi.",
                deptDaoTao, phuongAnh, 345, 110, Collections.emptyList()
        );
        initComment(p6, staffDaotao, "Cảm ơn Phương Anh đã chia sẻ thông tin rất hữu ích và chính xác cho các bạn sinh viên toàn trường!", 28);

        // 7. Bài viết 7: Phương án học lại Toán 2 (Trần Quốc Bảo)
        Post p7 = initForumPost(
                "Phương án học lại Toán 2 khi chương trình đào tạo chuyển đổi sang học phần Giải tích",
                "Mọi người cho mình hỏi, mình là sinh viên khóa cũ còn nợ môn Toán 2, nhưng kỳ này trường đổi tên và khung thành môn Giải tích thì mình đăng ký thế nào ạ? Có cần làm đơn học phần tương đương không?",
                deptDaoTao, quocBao, 178, 53, Collections.emptyList()
        );
        initComment(p7, staffDaotao, "Chào em, học phần Giải tích (3 tín chỉ) đã được Hội đồng Khoa học công nhận tương đương cho Toán 2. Khi đăng ký môn học trên trang dkmh em chọn trực tiếp môn tương đương nhé.", 31);

        // 8. Bài viết 8: Kinh nghiệm thực tập OJT ngành CNTT (Bùi Thanh Tùng)
        Post p8 = initForumPost(
                "Tổng hợp kinh nghiệm phỏng vấn thực tập doanh nghiệp (OJT) mảng Java / Spring Boot tại các công ty đối tác",
                "Vừa qua mình đã hoàn thành kỳ thực tập 4 tháng. Lời khuyên cho các bạn chuẩn bị đi OJT: 1) Nắm vững OOP, Java Core, Collections, Concurrency cơ bản; 2) Hiểu rõ Spring Data JPA, quan hệ OneToMany, ManyToOne và cơ chế transaction; 3) Kỹ năng sử dụng Git và RESTful API thiết kế chuẩn; 4) Thái độ cầu thị và chủ động hỏi mentor khi gặp blocker.",
                deptCntt, thanhTung, 289, 94, Collections.emptyList()
        );
        initComment(p8, giaHuy, "Bài viết rất thực tế và truyền cảm hứng cho lứa sinh viên K22 chuẩn bị đi thực tập!", 16);

        // 9. Bài viết 9: Săn học bổng khuyến khích học tập (Võ Thị Bích Ngân)
        Post p9 = initForumPost(
                "Kinh nghiệm ôn thi và đạt học bổng Khuyến khích học tập (HKKHT) loại Xuất sắc kỳ vừa rồi",
                "Chào các bạn, để săn được học bổng HKKHT ngoài việc duy trì điểm GPA trên 3.6 thì điểm rèn luyện (ĐRL) bắt buộc phải từ 90 điểm trở lên (loại Xuất sắc). Các bạn nên tham gia tích cực các hoạt động tình nguyện của Đoàn - Hội như Mùa hè xanh, Tiếp sức mùa thi và các hội thảo học thuật để cộng tối đa điểm rèn luyện nhé!",
                deptKt, bichNgan, 320, 88, Collections.emptyList()
        );
        initComment(p9, staffDoan, "Đoàn trường ghi nhận và đánh giá rất cao sự tích cực của Bích Ngân. Chúc em tiếp tục gặt hái nhiều thành tích cao!", 22);

        // 10. Bài viết 10: Mẹo thiết kế PCB và lập trình STM32 (Hoàng Minh Trí)
        Post p10 = initForumPost(
                "Mẹo thiết kế mạch in PCB 2 lớp và lập trình STM32 hạn chế nhiễu cho đồ án Vi xử lý",
                "Chia sẻ một số kinh nghiệm làm phần cứng: Luôn đặt tụ lọc nguồn 100nF gần sát chân VDD của MCU; đổ đồng GND cho cả 2 mặt PCB; tách biệt đường tín hiệu số tốc độ cao và đường analog ADC để tránh sai số đo lường.",
                deptDien, minhTri, 164, 48, Collections.emptyList()
        );

        // 11. Bài viết 11: Review môn học tự chọn AI vs Machine Learning (Đỗ Gia Huy)
        Post p11 = initForumPost(
                "Review các môn chuyên ngành tự chọn: Nhập môn Trí tuệ nhân tạo vs Học máy ứng dụng",
                "Các bạn K23 đang phân vân môn tự chọn kỳ tới có thể tham khảo: Môn Machine Learning thiên về toán thống kê, giải thuật gradient descent, SVM; còn môn Trí tuệ nhân tạo thực hành nhiều hơn về tìm kiếm đồ thị, heuristics và xử lý bài toán logic. Giảng viên cả 2 môn đều rất nhiệt tình hướng dẫn lab.",
                deptCntt, giaHuy, 215, 76, Collections.emptyList()
        );

        // 12. Bài viết 12: Tài liệu đồ án chi tiết máy (Phan Văn Hậu)
        Post p12 = initForumPost(
                "Nguồn tài liệu tham khảo và file mô phỏng 3D SolidWorks môn Thiết kế máy",
                "Mình vừa tổng hợp lại kho bài tập lớn và mô phỏng động học hộp giảm tốc 2 cấp đồng trục bằng SolidWorks và ANSYS. Bạn nào cần tham khảo để làm đồ án chi tiết máy thì comment bên dưới trao đổi nhé!",
                deptCkm, vanHau, 195, 62, Collections.emptyList()
        );

        log.info("Khởi tạo thành công 12 bài viết và các bình luận thực tế cho Diễn đàn sinh viên HCMUTE!");
    }

    private Post initOfficialAnnouncement(String title, String content, Department department, User author, int viewCount, int likeCount, boolean isPinned, String imageUrl) {
        if (postRepository.existsByTitle(title)) {
            return postRepository.findFirstByTitleContainingAndIsDeletedFalse(title).orElse(null);
        }

        Post post = Post.builder()
                .title(title)
                .content(content)
                .postType("OFFICIAL_ANNOUNCEMENT")
                .status("APPROVED")
                .department(department)
                .author(author)
                .approvedBy(author)
                .approvedAt(LocalDateTime.now())
                .viewCount(viewCount)
                .likeCount(likeCount)
                .isPinned(isPinned)
                .build();

        Post saved = postRepository.save(post);

        if (imageUrl != null && !imageUrl.isBlank()) {
            PostAttachment attachment = PostAttachment.builder()
                    .post(saved)
                    .fileName("banner_tintuc.jpg")
                    .fileUrl(imageUrl)
                    .fileType("IMAGE")
                    .fileSize(350000L)
                    .sourceType("DIRECT_UPLOAD")
                    .build();
            postAttachmentRepository.save(attachment);
        }
        return saved;
    }

    private void seedOfficialAnnouncementsFromHomepage(Department deptTuyenSinh, Department deptCntt, Department deptDaoTao,
                                                       User staffTuyensinh, User staffCntt, User staffDaotao, User staffAcademic) {
        log.info("Bắt đầu nạp 4 bài viết tin tức / thông báo chính thức tiêu điểm từ trang chủ vào Bảng tin...");

        // 1. Tuyển sinh 2026 (Ghim)
        initOfficialAnnouncement(
                "HCMUTE công bố phương án tuyển sinh đại học chính quy năm 2026",
                "<p>Hội đồng Tuyển sinh Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE) trân trọng thông báo phương án tuyển sinh đại học hệ chính quy năm 2026 với tổng chỉ tiêu dự kiến hơn 6.500 sinh viên cho 45 ngành đào tạo kỹ thuật, công nghệ, kinh tế và thiết kế mỹ thuật ứng dụng.</p>" +
                "<h4>1. Các phương thức xét tuyển chủ đạo:</h4>" +
                "<ul>" +
                "<li><strong>Phương thức 1:</strong> Xét tuyển thẳng và ưu tiên xét tuyển theo quy chế của Bộ Giáo dục và Đào tạo.</li>" +
                "<li><strong>Phương thức 2:</strong> Xét tuyển dựa trên kết quả kỳ thi Đánh giá năng lực của ĐHQG TP.HCM năm 2026 (chiếm 25 - 30% chỉ tiêu).</li>" +
                "<li><strong>Phương thức 3:</strong> Xét tuyển học bạ THPT kết hợp chứng chỉ ngoại ngữ quốc tế (IELTS từ 5.5 trở lên hoặc TOEIC từ 650 điểm).</li>" +
                "<li><strong>Phương thức 4:</strong> Xét tuyển dựa trên điểm thi Tốt nghiệp THPT năm 2026 theo các tổ hợp môn quy định.</li>" +
                "</ul>" +
                "<h4>2. Các ngành học và chuyên ngành mới đón đầu xu hướng:</h4>" +
                "<p>Năm 2026, Nhà trường mở mới và đẩy mạnh đào tạo các ngành đón đầu cuộc cách mạng công nghiệp 4.0 và bán dẫn: <em>Kỹ thuật Vi mạch Bán dẫn, Kỹ thuật Trí tuệ Nhân tạo & Khoa học Dữ liệu, Công nghệ Năng lượng Tái tạo</em>. Toàn bộ chương trình được kiểm định theo chuẩn quốc tế ABET và AUN-QA.</p>" +
                "<h4>3. Chính sách học bổng và hỗ trợ tài chính:</h4>" +
                "<p>HCMUTE dành quỹ học bổng trị giá hơn 40 tỷ đồng cho tân sinh viên có điểm xét tuyển thủ khoa, á khoa các ngành và học bổng khuyến khích tài năng kỹ thuật. Mọi thông tin chi tiết thí sinh có thể theo dõi tại cổng thông tin tuyển sinh hoặc liên hệ trực tiếp Bộ phận Tư vấn Tuyển sinh tại Phòng A1-101.</p>",
                deptTuyenSinh, staffTuyensinh, 3420, 418, true,
                "https://lh3.googleusercontent.com/aida-public/AB6AXuD1Ds92ZUE1RuZXEH1z8Yrll-VXSeiXEnLJNIeCH0bQc647wKq-iS9BEX3UIsGT6LwL2yUoMo4fdvWH6QD-0XtmoVP9sKk4cOAgjtbNwbRX1mI3QdMEt9owTtNPlQcfobKUYKiP3xlL-UG8Qfano_p-O46pdXvKNeje3P5-FbXxYfco5obx9m81-1S3hw8pFIl4AisJo4KxuObjk_SJ-VgL-QEa--fNN79giqmieQZasDJX6QIblw8G"
        );

        // 2. Nghiên cứu sáng tạo kỹ thuật
        initOfficialAnnouncement(
                "Sinh viên HCMUTE đạt giải cao tại Cuộc thi Sáng tạo Kỹ thuật toàn quốc 2026",
                "<p>Vừa qua, tại Vòng Chung kết Cuộc thi Sáng tạo Khoa học Kỹ thuật Toàn quốc (VIFOTEC) lần thứ 32 tổ chức tại Hà Nội, đoàn cán bộ và sinh viên Trường Đại học Sư phạm Kỹ thuật TP.HCM đã xuất sắc giành 01 Giải Nhất và 02 Giải Nhì ở các bảng đấu Công nghệ Thông tin và Cơ điện tử thông minh.</p>" +
                "<h4>Dấu ấn công nghệ đột phá:</h4>" +
                "<p>Đề tài xuất sắc đạt Giải Nhất thuộc về nhóm sinh viên liên ngành CNTT & CKM với công trình: <em>'Hệ thống phân loại phế phẩm quang học tốc độ cao ứng dụng thị giác máy tính và cánh tay Robot SCARA Delta'</em> do TS. Trần Khoa CNTT hướng dẫn. Đề tài được Hội đồng giám khảo đánh giá rất cao về tính ứng dụng thực tiễn, khả năng chuyển giao công nghệ cho các nhà máy tự động hóa trong nước với chi phí chỉ bằng 1/3 thiết bị nhập ngoại.</p>" +
                "<h4>Chính sách khen thưởng từ Nhà trường:</h4>" +
                "<p>Hiệu trưởng Nhà trường đã ký quyết định tặng Giấy khen và tiền thưởng biểu dương tinh thần sáng tạo, dám nghĩ dám làm của các đội thi. Sự kiện này một lần nữa khẳng định triết lý đào tạo thực hành - khai phóng và chất lượng nghiên cứu ứng dụng hàng đầu của sinh viên HCMUTE.</p>",
                deptCntt, staffCntt, 1875, 289, false,
                "https://lh3.googleusercontent.com/aida-public/AB6AXuC07M2Qnaq-8FmJ3hQS9WLRrG_K_Gc8Zyo8JoiuebrgOBYjC1LSEfAH_zKlf87wGVtSFz8LTe93SqYct6cU8MU5ZmshwfF7HYwu9pEsRG6s2d8tWkcBW02cbu_YBHz3pbR55nxVcq9LsmvwK0S0hMjzpJSfB2Y9JxhbHbQqfd1M7I9yZjCGNXZeH65gBtrSk6CjrEzC3fZAtzk69gLyThnSjqBLsmMiZ8bNehexvxThIZcESPPiJ3hV"
        );

        // 3. Ngày hội việc làm kết nối 150 doanh nghiệp
        initOfficialAnnouncement(
                "HCMUTE tổ chức Ngày hội việc làm năm 2026 kết nối hơn 150 doanh nghiệp",
                "<p>Nhằm tạo cầu nối vững chắc giữa sinh viên chuẩn bị tốt nghiệp với thị trường lao động chất lượng cao, Trường Đại học Sư phạm Kỹ thuật TP.HCM phối hợp cùng hơn 150 tập đoàn công nghệ, doanh nghiệp đa quốc gia tổ chức <strong>Ngày hội Việc làm & Kết nối Doanh nghiệp HCMUTE Job Fair 2026</strong>.</p>" +
                "<h4>Quy mô ngày hội:</h4>" +
                "<ul>" +
                "<li>Hơn <strong>5.000 vị trí tuyển dụng</strong> kỹ sư kỹ thuật, lập trình viên phần mềm, chuyên viên kinh tế, logistics và thiết kế đồ họa.</li>" +
                "<li>Sự tham gia của các đối tác chiến lược hàng đầu: Samsung, Bosch, Intel Products Vietnam, FPT Software, TMA Solutions, Thaco Group, VinFast...</li>" +
                "<li>Chuỗi workshop phỏng vấn thử (Mock Interview), sửa CV trực tiếp 1:1 cùng các Giám đốc Nhân sự (HRD) kỳ cựu.</li>" +
                "</ul>" +
                "<h4>Thời gian và địa điểm:</h4>" +
                "<p><strong>Thời gian:</strong> 07h30 - 16h30, Thứ Bảy ngày 18/10/2026.<br>" +
                "<strong>Địa điểm:</strong> Sân bóng đá trung tâm và Hội trường lớn Khu A, Trường ĐH Sư phạm Kỹ thuật TP.HCM (Số 01 Võ Văn Ngân, TP. Thủ Đức).<br>" +
                "Sinh viên mang theo thẻ sinh viên và bản in CV để tham gia ứng tuyển trực tiếp.</p>",
                deptDaoTao, staffDaotao, 2650, 340, false,
                "https://lh3.googleusercontent.com/aida-public/AB6AXuB2mzpwjo8xW1O7waS4y5vuF5KXT2KKPIzpsW2YgE_Lnkb8wRYEjMIutZeiyWVqOJOAtWfgnmy-DPzl6bar-TzqzWkT-oozFwse_o0ohjTDWMPWsv-XH30jSvpTohcbZlVrLkb6-beZ0TNRDEWvk5wIbpB6JtC6vSoytWIaEGNkavGmdiBcFdlbg10-mnHu-wsq0K3NwLxyZMWA84SOk2SW2NChnc5_lR4fN02bM9JuapNr_p98dZzp"
        );

        // 4. Hợp tác quốc tế
        initOfficialAnnouncement(
                "Ký kết hợp tác chiến lược đào tạo & nghiên cứu với các trường đại học quốc tế",
                "<p>Sáng ngày 15/09/2026, tại Tòa nhà Trung tâm Trường Đại học Sư phạm Kỹ thuật TP.HCM đã long trọng diễn ra Lễ ký kết Thỏa thuận Hợp tác Chiến lược (MoU) về Đào tạo song bằng và Nghiên cứu khoa học chuyển giao công nghệ giữa HCMUTE và Liên minh các Trường Đại học Kỹ thuật Công nghệ Quốc tế (Hàn Quốc, Nhật Bản, Đài Loan).</p>" +
                "<h4>Nội dung hợp tác trọng điểm:</h4>" +
                "<ul>" +
                "<li><strong>Chương trình trao đổi sinh viên (Student Exchange):</strong> Mỗi năm cấp 30 suất học bổng toàn phần du học ngắn hạn (1 - 2 học kỳ) cho sinh viên xuất sắc các ngành Cơ điện tử, CNTT và Kỹ thuật Hóa học.</li>" +
                "<li><strong>Chương trình liên kết đào tạo 2+2 và 3+1:</strong> Sinh viên hoàn thành giai đoạn đầu tại HCMUTE và chuyển tiếp nhận bằng Cử nhân/Kỹ sư quốc tế.</li>" +
                "<li><strong>Thành lập Phòng Thí nghiệm Chung (Joint Lab):</strong> Nghiên cứu công nghệ Trí tuệ Nhân tạo thế hệ mới và chuyển đổi số công nghiệp sản xuất thông minh.</li>" +
                "</ul>" +
                "<p>Buổi lễ ký kết là bước tiến quan trọng trong chiến lược quốc tế hóa giáo dục đại học của HCMUTE, mang lại môi trường học tập đẳng cấp quốc tế ngay tại Việt Nam.</p>",
                deptDaoTao, staffAcademic, 1420, 215, false,
                "https://lh3.googleusercontent.com/aida-public/AB6AXuDYwKvDUq7Prg4KyChK5ZlVy31N8K2tMYtVLbNctJdAdvsXL0xyc6RSP_8GCPfwVsufPBlBa6yBdQ6KZ-DpUoCjMB6xozC-57VFg46mwNPuf3YqEgNVc4ystcWvQMJHzILxNOLuAFYw_MdrM2VkU9ISRurtRtcecwE84Xpn44yLvM1Uf9-Wfq9fhFkcPfOf6d5yE8M7jUuvn36rO5I_and6BLW2478qO_IFIr3isl7-RyoBHzpkhqif"
        );

        log.info("Khởi tạo thành công 4 bài viết thông báo chính thức tiêu điểm từ trang chủ!");
    }

    private void seed20OfficialRegulations() {
        try {
            File file = new File("docs/dataset/seed_20_official_regulations.json");
            if (!file.exists()) {
                log.info("Không tìm thấy tệp dataset 20 công văn tại {}, bỏ qua nạp công văn", file.getPath());
                return;
            }

            List<Map<String, Object>> records = objectMapper.readValue(file, new TypeReference<>() {});
            int addedCount = 0;

            for (Map<String, Object> r : records) {
                String title = (String) r.get("title");
                if (title == null || title.isBlank() || postRepository.existsByTitle(title)) {
                    continue;
                }

                Number deptIdNum = (Number) r.get("dept_id");
                Number authorIdNum = (Number) r.get("author_id");
                Number viewsNum = (Number) r.get("views");
                Number likesNum = (Number) r.get("likes");
                Number isPinnedNum = (Number) r.get("is_pinned");
                Number fileSizeNum = (Number) r.get("file_size");

                Department dept = deptIdNum != null ? departmentRepository.findById(deptIdNum.longValue()).orElse(null) : null;
                User author = authorIdNum != null ? userRepository.findById(authorIdNum.longValue()).orElse(null) : null;

                String content = (String) r.get("content");
                String fileNameDisplay = (String) r.get("file_name_display");
                String filePath = (String) r.get("file_path");

                Post post = Post.builder()
                        .title(title)
                        .content(content)
                        .postType("OFFICIAL_ANNOUNCEMENT")
                        .status("APPROVED")
                        .department(dept)
                        .author(author)
                        .approvedBy(author)
                        .approvedAt(LocalDateTime.now())
                        .viewCount(viewsNum != null ? viewsNum.intValue() : 1000)
                        .likeCount(likesNum != null ? likesNum.intValue() : 50)
                        .isPinned(isPinnedNum != null && isPinnedNum.intValue() == 1)
                        .build();

                Post saved = postRepository.save(post);

                if (filePath != null && !filePath.isBlank()) {
                    PostAttachment attachment = PostAttachment.builder()
                            .post(saved)
                            .fileName(fileNameDisplay != null ? fileNameDisplay : "cong_van.pdf")
                            .fileUrl(filePath)
                            .fileType("PDF")
                            .fileSize(fileSizeNum != null ? fileSizeNum.longValue() : 500000L)
                            .sourceType("LOCAL_PATH")
                            .build();
                    postAttachmentRepository.save(attachment);
                }
                addedCount++;
            }

            if (addedCount > 0) {
                log.info("Nạp thành công {} công văn học vụ chính thức vào Bảng tin!", addedCount);
            }
        } catch (Exception e) {
            log.error("Lỗi khi nạp 20 công văn chính thức từ dataset: ", e);
        }
    }

    /**
     * Dọn dẹp triệt để các tài khoản cán bộ giả lập từ bài test race condition (race_staff_1 -> race_staff_10).
     */
    private void cleanupMockRaceStaffUsers() {
        try {
            log.info("Kiểm tra và dọn dẹp các tài khoản cán bộ thử nghiệm race_staff_*...");
            jdbcTemplate.update("UPDATE tickets SET assigned_to = NULL WHERE assigned_to IN (SELECT id FROM users WHERE username LIKE 'race_staff_%' OR email LIKE 'race_staff_%')");
            jdbcTemplate.update("UPDATE ticket_histories SET actor_id = NULL WHERE actor_id IN (SELECT id FROM users WHERE username LIKE 'race_staff_%' OR email LIKE 'race_staff_%')");
            int deleted = jdbcTemplate.update("DELETE FROM users WHERE username LIKE 'race_staff_%' OR email LIKE 'race_staff_%'");
            if (deleted > 0) {
                log.info("==> Đã xóa thành công {} tài khoản cán bộ test (race_staff_1 -> race_staff_10).", deleted);
            }
        } catch (Exception e) {
            log.warn("Không thể xóa tài khoản test race_staff: {}", e.getMessage());
        }
    }

    /**
     * Dọn dẹp triệt để các bình luận trùng lặp do server restart / reload nhiều lần.
     */
    private void cleanupDuplicateComments() {
        try {
            int deleted = jdbcTemplate.update(
                "DELETE c1 FROM post_comments c1 INNER JOIN post_comments c2 " +
                "WHERE c1.id > c2.id AND c1.post_id = c2.post_id AND c1.author_id = c2.author_id AND c1.content = c2.content"
            );
            if (deleted > 0) {
                log.info("==> Đã dọn dẹp {} bình luận trùng lặp trên Diễn đàn sinh viên!", deleted);
            }
        } catch (Exception e) {
            log.warn("Không thể dọn dẹp bình luận trùng lặp: {}", e.getMessage());
        }
    }
}

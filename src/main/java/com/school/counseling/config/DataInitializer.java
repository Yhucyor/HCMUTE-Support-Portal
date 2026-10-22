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

    public DataInitializer(RoleRepository roleRepository,
                           DepartmentRepository departmentRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           FaqRepository faqRepository,
                           SmartFaqMatcherService smartFaqMatcherService,
                           ObjectMapper objectMapper,
                           PostRepository postRepository,
                           CommentRepository commentRepository,
                           PostAttachmentRepository postAttachmentRepository) {
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
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Bắt đầu kiểm tra và khởi tạo dữ liệu mẫu hệ thống...");

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

            log.info("Khởi tạo dữ liệu mẫu Auth, FAQ & Diễn đàn sinh viên thành công!");
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
        return userRepository.findByUsername(username).orElseGet(() -> {
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
}

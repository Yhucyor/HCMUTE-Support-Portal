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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Tự động khởi tạo dữ liệu mẫu (Roles, Departments, Sample Users, FAQ Dataset) khi ứng dụng
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

    public DataInitializer(RoleRepository roleRepository,
                           DepartmentRepository departmentRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           FaqRepository faqRepository,
                           SmartFaqMatcherService smartFaqMatcherService,
                           ObjectMapper objectMapper) {
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.faqRepository = faqRepository;
        this.smartFaqMatcherService = smartFaqMatcherService;
        this.objectMapper = objectMapper;
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

            // 3. Khởi tạo Tài khoản mẫu (Mật khẩu: 123456 hoặc Password123@)
            String defaultPassword = "123456";

            // Tài khoản Quản trị viên
            initUser("admin", "123456", "Quản Trị Viên Hệ Thống", "admin@hcmute.edu.vn", roleAdmin, null);
            initUser("admin_main", "123456", "Admin Quản Trị Cấp Cao", "admin@gmail.com", roleAdmin, null);

            // Tài khoản Cán bộ 5 đơn vị
            initUser("staff_academic", defaultPassword, "Cán bộ Quản lý Đào tạo", "staff.academic@hcmute.edu.vn",
                    roleStaff, deptDaoTao);
            initUser("staff_daotao", defaultPassword, "ThS. Phạm Thị Thu Sương", "daotao.staff@hcmute.edu.vn",
                    roleStaff, deptDaoTao);
            initUser("staff_tuyensinh", defaultPassword, "ThS. Nguyễn Văn Tuyển Sinh", "tuyensinh.staff@hcmute.edu.vn",
                    roleStaff, deptTuyenSinh);
            initUser("staff_cntt", defaultPassword, "TS. Trần Khoa CNTT", "cntt.staff@hcmute.edu.vn", roleStaff,
                    deptCntt);
            initUser("staff_doan", defaultPassword, "Đ/c Lê Bí Thư Đoàn", "doantn.staff@hcmute.edu.vn", roleStaff,
                    deptDoan);
            initUser("staff_nn", defaultPassword, "ThS. Giảng Viên Ngoại Ngữ", "nn.staff@hcmute.edu.vn", roleStaff,
                    deptNn);

            // Tài khoản Sinh viên
            initUser("student", defaultPassword, "Nguyễn Văn Sinh Viên Mẫu", "student@student.hcmute.edu.vn", roleStudent, deptCntt);
            initUser("student01", "123456", "Nguyễn Văn Sinh Viên", "student01@hcmute.edu.vn", roleStudent, deptCntt);
            initUser("lecuong", "123456", "Lê Quốc Cường", "lecuong@gmail.com", roleStudent, deptCntt);

            // 4. Khởi tạo / Nạp Kho Tri Thức FAQs từ docs/dataset/faq_dataset_curated.json
            if (faqRepository.count() == 0) {
                log.info("Kho FAQ chưa khởi tạo. Tiến hành nạp kho tri thức FAQ...");
                seedFaqsFromDataset(deptDoan, deptTuyenSinh, deptDaoTao, deptCntt, deptNn);
                smartFaqMatcherService.reloadCache();
            }

            log.info("Khởi tạo dữ liệu mẫu Auth & FAQ thành công!");
        } catch (Exception e) {
            log.error("Lỗi trong quá trình khởi tạo dữ liệu mẫu: ", e);
        }
    }

    private void seedFaqsFromDataset(Department deptDoan, Department deptTuyenSinh, Department deptDaoTao, Department deptCntt, Department deptNn) {
        try {
            File file = new File("docs/dataset/faq_dataset_curated.json");
            if (!file.exists()) {
                log.info("Không tìm thấy tệp dataset FAQ tại docs/dataset/faq_dataset_curated.json, bỏ qua nạp FAQ");
                return;
            }

            log.info("Bắt đầu nạp kho tri thức FAQ từ {}...", file.getPath());
            List<Map<String, Object>> records = objectMapper.readValue(file, new TypeReference<>() {});
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
            }

            if (!entities.isEmpty()) {
                faqRepository.saveAll(entities);
                log.info("Đã nạp thành công {} câu hỏi FAQ tinh tuyển vào Database!", entities.size());
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

    private void initUser(String username, String rawPassword, String fullName, String email, Role role,
            Department department) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName(fullName)
                    .email(email)
                    .role(role)
                    .department(department)
                    .status(UserStatus.ACTIVE)
                    .avatarUrl("https://ui-avatars.com/api/?name=" + fullName.replace(" ", "+")
                            + "&background=0D8ABC&color=fff")
                    .build();
            userRepository.save(user);
            log.info("Đã tạo tài khoản mẫu: {} | Mật khẩu: {}", username, rawPassword);
        }
    }
}

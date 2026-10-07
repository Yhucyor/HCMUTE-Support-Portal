package com.school.counseling.config;

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

/**
 * Tự động khởi tạo dữ liệu mẫu (Roles, Departments, Sample Users) khi ứng dụng
 * chạy lần đầu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
            initUser("student01", "123456", "Nguyễn Văn Sinh Viên", "student01@hcmute.edu.vn", roleStudent, deptCntt);
            initUser("lecuong", "123456", "Lê Quốc Cường", "lecuong@gmail.com", roleStudent, deptCntt);

            log.info("Khởi tạo dữ liệu mẫu Auth (Roles, Departments, Sample Accounts) thành công!");
        } catch (Exception e) {
            log.error("Lỗi trong quá trình khởi tạo dữ liệu mẫu: ", e);
        }
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

package com.school.counseling.module.admin.service;

import com.school.counseling.common.exception.ResourceNotFoundException;
import com.school.counseling.common.util.SecurityUtils;
import com.school.counseling.module.admin.dto.UpdateUserAdminDto;
import com.school.counseling.module.admin.dto.UserManagementDto;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.Role;
import com.school.counseling.module.auth.entity.User;
import com.school.counseling.module.auth.entity.UserStatus;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.RoleRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;

    public AdminUserService(UserRepository userRepository, RoleRepository roleRepository, DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
    }

    public Page<UserManagementDto> getUsers(String keyword, Long roleId, Long departmentId, String status, Pageable pageable) {
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        UserStatus userStatus = null;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            try {
                userStatus = UserStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<User> page = userRepository.searchUsers(kw, roleId, departmentId, userStatus, pageable);
        return page.map(this::mapToDto);
    }

    public UserManagementDto getUserById(Long id) {
        User user = userRepository.findByIdWithRoleAndDepartment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
        return mapToDto(user);
    }

    @Transactional
    public UserManagementDto toggleUserLock(Long id) {
        User user = userRepository.findByIdWithRoleAndDepartment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        Long currentAdminId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentAdminId != null && currentAdminId.equals(id)) {
            throw new IllegalArgumentException("Quản trị viên không thể tự khóa tài khoản của chính mình!");
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
            log.info("Admin mở khóa tài khoản user ID: {}, username: {}", id, user.getUsername());
        } else {
            user.setStatus(UserStatus.LOCKED);
            log.info("Admin khóa tài khoản user ID: {}, username: {}", id, user.getUsername());
        }

        User saved = userRepository.save(user);
        return mapToDto(saved);
    }

    @Transactional
    public UserManagementDto updateUser(Long id, UpdateUserAdminDto dto) {
        User user = userRepository.findByIdWithRoleAndDepartment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        user.setFullName(dto.getFullName().trim());

        String normalizedEmail = dto.getEmail().trim();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new IllegalArgumentException("Email [" + normalizedEmail + "] đã được sử dụng bởi tài khoản khác!");
        }
        user.setEmail(normalizedEmail);
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);

        if (dto.getRoleId() != null) {
            Role role = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với ID: " + dto.getRoleId()));
            user.setRole(role);
        }

        if (dto.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Khoa/Phòng với ID: " + dto.getDepartmentId()));
            user.setDepartment(dept);
        } else {
            user.setDepartment(null);
        }

        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            try {
                user.setStatus(UserStatus.valueOf(dto.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Trạng thái người dùng [" + dto.getStatus() + "] không hợp lệ! (Cho phép: ACTIVE, LOCKED, PENDING_ACTIVATION)");
            }
        }

        User updated = userRepository.save(user);
        log.info("Admin cập nhật thông tin user ID: {}, username: {}", id, updated.getUsername());
        return mapToDto(updated);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        Long currentAdminId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentAdminId != null && currentAdminId.equals(id)) {
            throw new IllegalArgumentException("Quản trị viên không thể tự xóa tài khoản của chính mình!");
        }

        try {
            userRepository.delete(user);
            log.info("Admin đã xóa hoàn toàn tài khoản người dùng ID: {}, username: {}", id, user.getUsername());
        } catch (Exception ex) {
            user.setStatus(UserStatus.LOCKED);
            userRepository.save(user);
            log.info("Do có dữ liệu liên kết, tài khoản user ID: {}, username: {} đã được chuyển về trạng thái LOCKED", id, user.getUsername());
        }
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    private UserManagementDto mapToDto(User user) {
        return UserManagementDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .roleName(user.getRole() != null ? user.getRole().getName() : "ROLE_STUDENT")
                .roleDescription(user.getRole() != null ? user.getRole().getDescription() : "")
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : "Chưa phân bổ")
                .status(user.getStatus() != null ? user.getStatus().name() : "ACTIVE")
                .createdAt(user.getCreatedAt())
                .build();
    }
}

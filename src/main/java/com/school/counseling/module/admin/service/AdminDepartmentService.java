package com.school.counseling.module.admin.service;

import com.school.counseling.common.exception.ResourceNotFoundException;
import com.school.counseling.module.admin.dto.DepartmentAdminDto;
import com.school.counseling.module.admin.dto.SaveDepartmentDto;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import com.school.counseling.module.auth.repository.UserRepository;
import com.school.counseling.module.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminDepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public AdminDepartmentService(DepartmentRepository departmentRepository, UserRepository userRepository, TicketRepository ticketRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<DepartmentAdminDto> getAllDepartmentsWithStats() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<DepartmentAdminDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDtoSimple)
                .collect(Collectors.toList());
    }

    public Optional<DepartmentAdminDto> findDepartmentDtoById(Long id) {
        return departmentRepository.findById(id).map(this::mapToDtoSimple);
    }

    public DepartmentAdminDto getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Đơn vị với ID: " + id));
        return mapToDto(dept);
    }

    @Transactional
    public DepartmentAdminDto saveDepartment(SaveDepartmentDto dto) {
        String normalizedCode = dto.getCode().trim().toUpperCase();
        String normalizedName = dto.getName().trim();

        Department dept;
        if (dto.getId() != null) {
            if (departmentRepository.existsByCodeAndIdNot(normalizedCode, dto.getId())) {
                throw new IllegalArgumentException("Mã Đơn vị [" + normalizedCode + "] đã được sử dụng bởi đơn vị khác!");
            }
            if (departmentRepository.existsByNameAndIdNot(normalizedName, dto.getId())) {
                throw new IllegalArgumentException("Tên Đơn vị [" + normalizedName + "] đã tồn tại trên hệ thống!");
            }
            dept = departmentRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Đơn vị với ID: " + dto.getId()));
        } else {
            if (departmentRepository.existsByCode(normalizedCode)) {
                throw new IllegalArgumentException("Mã Đơn vị [" + normalizedCode + "] đã tồn tại trên hệ thống!");
            }
            if (departmentRepository.existsByName(normalizedName)) {
                throw new IllegalArgumentException("Tên Đơn vị [" + normalizedName + "] đã tồn tại trên hệ thống!");
            }
            dept = new Department();
        }

        dept.setName(normalizedName);
        dept.setCode(normalizedCode);
        dept.setOfficeLocation(dto.getOfficeLocation() != null ? dto.getOfficeLocation().trim() : null);
        dept.setContactEmail(dto.getContactEmail() != null ? dto.getContactEmail().trim() : null);
        dept.setContactPhone(dto.getContactPhone() != null ? dto.getContactPhone().trim() : null);
        dept.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        dept.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        Department saved = departmentRepository.save(dept);
        log.info("Admin lưu thông tin Đơn vị: ID={}, Name={}, Code={}", saved.getId(), saved.getName(), saved.getCode());
        return mapToDto(saved);
    }

    @Transactional
    public DepartmentAdminDto toggleDepartmentStatus(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Đơn vị với ID: " + id));

        dept.setIsActive(!Boolean.TRUE.equals(dept.getIsActive()));
        Department saved = departmentRepository.save(dept);
        log.info("Admin đổi trạng thái Đơn vị ID: {}, isActive: {}", id, saved.getIsActive());
        return mapToDto(saved);
    }

    private DepartmentAdminDto mapToDto(Department d) {
        long staffCount = userRepository.countByDepartmentId(d.getId());
        long ticketCount = ticketRepository.countByDepartmentId(d.getId());

        return DepartmentAdminDto.builder()
                .id(d.getId())
                .name(d.getName())
                .code(d.getCode())
                .officeLocation(d.getOfficeLocation())
                .contactEmail(d.getContactEmail())
                .contactPhone(d.getContactPhone())
                .description(d.getDescription())
                .isActive(d.getIsActive())
                .staffCount(staffCount)
                .totalTickets(ticketCount)
                .createdAt(d.getCreatedAt())
                .build();
    }

    private DepartmentAdminDto mapToDtoSimple(Department d) {
        return DepartmentAdminDto.builder()
                .id(d.getId())
                .name(d.getName())
                .code(d.getCode())
                .officeLocation(d.getOfficeLocation())
                .contactEmail(d.getContactEmail())
                .contactPhone(d.getContactPhone())
                .description(d.getDescription())
                .isActive(d.getIsActive())
                .createdAt(d.getCreatedAt())
                .build();
    }
}

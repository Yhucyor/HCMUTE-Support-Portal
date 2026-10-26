package com.school.counseling.module.auth.service;

import com.school.counseling.config.CacheConfig;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service truy vấn danh mục Khoa / Phòng Ban áp dụng Caffeine Cache L1
 * Giúp loại bỏ hoàn toàn các truy vấn lặp đi lặp lại qua mạng tới TiDB Cloud khi chuyển trang
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentQueryService {

    private final DepartmentRepository departmentRepository;

    /**
     * Lấy danh sách toàn bộ phòng ban / khoa đang hoạt động (Được Cache 30 phút trong RAM)
     */
    @Cacheable(value = CacheConfig.CACHE_DEPARTMENTS_ACTIVE, unless = "#result == null || #result.isEmpty()")
    public List<Department> getActiveDepartments() {
        log.debug("💾 [Cache Miss] Đang nạp danh mục Departments từ Database lên Caffeine RAM...");
        return departmentRepository.findByIsActiveTrue();
    }

    /**
     * Xóa cache khi có cập nhật hoặc thay đổi phòng ban từ phía Admin
     */
    @CacheEvict(value = CacheConfig.CACHE_DEPARTMENTS_ACTIVE, allEntries = true)
    public void evictDepartmentCache() {
        log.info("🧹 [Cache Evict] Đã xóa cache danh mục Departments.");
    }
}

package com.school.counseling.module.auth.repository;

import com.school.counseling.module.auth.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);
    List<Department> findByIsActiveTrue();
    boolean existsByCode(String code);
}

package com.school.counseling.module.auth.entity;

import com.school.counseling.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Department extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // e.g. DOAN_HOI, TUYEN_SINH, DAO_TAO, KHOA_CNTT, KHOA_NN

    @Column(name = "office_location", length = 100)
    private String officeLocation;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @lombok.Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

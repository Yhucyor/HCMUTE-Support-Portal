package com.school.counseling.module.auth.entity;

import com.school.counseling.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name; // e.g. ROLE_STUDENT, ROLE_STAFF, ROLE_ADMIN

    @Column(name = "description", length = 255)
    private String description;
}

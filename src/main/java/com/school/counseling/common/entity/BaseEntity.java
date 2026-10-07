package com.school.counseling.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base Entity chung quản lý khóa chính và metadata kiểm toán
 * các bảng trong db đều kếthuawf từ file này ko phải viét lặp lại các cột
 * update
 */
@Getter
@Setter
@NoArgsConstructor // tạo constructor rỗng BaseEntity() { }
@AllArgsConstructor // tạo constructor đủ tham số
@SuperBuilder // Builder design pattern dùng để khởi tạo đối tượng
@MappedSuperclass // BaseEntity sẽ ko tạo bảng riêng mà chỉ là các cột được nhét vào bảng con
public abstract class BaseEntity implements Serializable {

    @Id // đánh dấu là khóa chính của bảng
    @GeneratedValue(strategy = GenerationType.IDENTITY) // tự động tăng
    @Column(name = "id", nullable = false, updatable = false) // id ko đc rỗng ko đc thay đổi
    private Long id;

    @CreationTimestamp // khi tạo mới thì tự động lấy thời gian hiện tại
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @lombok.Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @PrePersist // hàm tièn xử lý chốt chặn an toàn cuối
    protected void onCreate() {
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.createdBy == null) {
            this.createdBy = "SYSTEM";
        }
    }
}
// csdl thế giới cột bảng dòng quan hệ relational
// java là class object ds list , ooop
// entity bản phiên dịch orm object relational mapping
// chạy userRepo.findById(1)
// hibernate xuống mysql select from user where id = 1 ;
// hibernate nhìn vào file User bốc từng cột trong sql nhắc vào biến id ,
// createdAt , fullNam của java object

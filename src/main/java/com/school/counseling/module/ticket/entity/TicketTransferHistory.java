package com.school.counseling.module.ticket.entity;

import com.school.counseling.common.entity.BaseEntity;
import com.school.counseling.module.auth.entity.Department;
import com.school.counseling.module.auth.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "ticket_transfer_histories", indexes = {
    @Index(name = "idx_tth_ticket_id", columnList = "ticket_id"),
    @Index(name = "idx_tth_from_dept", columnList = "from_department_id"),
    @Index(name = "idx_tth_to_dept", columnList = "to_department_id")
})
@SQLDelete(sql = "UPDATE ticket_transfer_histories SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TicketTransferHistory extends BaseEntity {

    @NotNull(message = "Ticket không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @NotNull(message = "Đơn vị chuyển đi không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_department_id", nullable = false)
    private Department fromDepartment;

    @NotNull(message = "Đơn vị tiếp nhận mới không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_department_id", nullable = false)
    private Department toDepartment;

    @NotNull(message = "Người thực hiện chuyển giao không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferred_by_user_id", nullable = false)
    private User transferredBy;

    @NotBlank(message = "Lý do chuyển giao không được để trống")
    @Size(max = 500, message = "Lý do chuyển giao tối đa 500 ký tự")
    @Column(name = "reason", length = 500, nullable = false)
    private String reason;
}

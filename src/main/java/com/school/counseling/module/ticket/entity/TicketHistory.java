package com.school.counseling.module.ticket.entity;

import com.school.counseling.common.entity.BaseEntity;
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
@Table(name = "ticket_histories")
@SQLDelete(sql = "UPDATE ticket_histories SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TicketHistory extends BaseEntity {

    @NotNull(message = "Ticket không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @NotBlank(message = "Tên người thực hiện không được để trống")
    @Size(max = 150, message = "Tên người thực hiện tối đa 150 ký tự")
    @Column(name = "actor_name", length = 150, nullable = false)
    private String actorName;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @NotBlank(message = "Trạng thái mới không được để trống")
    @Column(name = "to_status", length = 30, nullable = false)
    private String toStatus;

    @Column(name = "action_note", columnDefinition = "TEXT")
    private String actionNote;
}

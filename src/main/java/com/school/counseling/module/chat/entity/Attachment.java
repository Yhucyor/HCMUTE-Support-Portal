package com.school.counseling.module.chat.entity;

import com.school.counseling.common.entity.BaseEntity;
import com.school.counseling.module.ticket.entity.Ticket;
import com.school.counseling.module.ticket.entity.TicketHistory;
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
@Table(name = "attachments")
@SQLDelete(sql = "UPDATE attachments SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Attachment extends BaseEntity {

    @NotBlank(message = "Tên tệp không được để trống")
    @Size(max = 255, message = "Tên tệp tối đa 255 ký tự")
    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @NotBlank(message = "Đường dẫn tệp không được để trống")
    @Size(max = 500, message = "Đường dẫn tối đa 500 ký tự")
    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @NotBlank(message = "Loại tệp không được để trống")
    @Size(max = 50, message = "Loại tệp tối đa 50 ký tự")
    @Column(name = "file_type", length = 50, nullable = false)
    private String fileType;

    @NotNull(message = "Dung lượng tệp không được để trống")
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_history_id")
    private TicketHistory ticketHistory;
}

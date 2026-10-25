package com.school.counseling.module.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTransferRequest {

    @NotNull(message = "Đơn vị tiếp nhận mới không được để trống")
    private Long toDepartmentId;

    @NotBlank(message = "Lý do chuyển giao không được để trống")
    @Size(max = 500, message = "Lý do chuyển giao tối đa 500 ký tự")
    private String reason;
}

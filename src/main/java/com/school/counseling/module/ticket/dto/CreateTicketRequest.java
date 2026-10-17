package com.school.counseling.module.ticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketRequest {

    @NotBlank(message = "Tiêu đề yêu cầu không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung yêu cầu không được để trống")
    private String description;

    @NotNull(message = "Đơn vị tiếp nhận không được để trống")
    private Long departmentId;

    private Long conversationId;

    @Builder.Default
    private String priority = "MEDIUM";

    private String topicCategory;

    @Size(max = 150, message = "Họ tên người gửi tối đa 150 ký tự")
    private String guestName;

    @Email(message = "Định dạng email không hợp lệ")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String guestEmail;
}
package com.school.counseling.module.academic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HcmuteSyncRequest {

    @NotBlank(message = "Mã số sinh viên không được để trống")
    private String studentId;

    @NotBlank(message = "Mật khẩu cổng đào tạo không được để trống")
    private String password;
}

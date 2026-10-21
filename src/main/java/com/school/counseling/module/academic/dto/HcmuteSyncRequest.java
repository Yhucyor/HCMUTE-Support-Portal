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

    private String password;
}

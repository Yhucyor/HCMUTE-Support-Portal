package com.school.counseling.module.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDepartmentDto {
    private Long id;

    @NotBlank(message = "Tên đơn vị không được để trống")
    @Size(max = 150, message = "Tên đơn vị tối đa 150 ký tự")
    private String name;

    @NotBlank(message = "Mã đơn vị không được để trống")
    @Size(max = 50, message = "Mã đơn vị tối đa 50 ký tự")
    private String code;

    @Size(max = 100, message = "Văn phòng tối đa 100 ký tự")
    private String officeLocation;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String contactEmail;

    @Size(max = 50, message = "Số điện thoại tối đa 50 ký tự")
    private String contactPhone;

    private String description;

    @Builder.Default
    private Boolean isActive = true;
}

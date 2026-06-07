package com.ziyara.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPortalStaffPasswordRequest {

    @NotBlank
    @Size(min = 6, max = 128)
    private String newPassword;
}

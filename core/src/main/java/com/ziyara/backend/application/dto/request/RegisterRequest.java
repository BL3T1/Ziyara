package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for user registration (public).
 */
@Data
@Schema(description = "Registration request")
public class RegisterRequest {

    @NotBlank
    @Email
    @Schema(description = "Email", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank
    @Size(min = 6)
    @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String password;

    @Schema(description = "Phone number")
    private String phone;

    @NotNull
    @Schema(description = "Role (typically CUSTOMER for self-registration)", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserRole role;
}

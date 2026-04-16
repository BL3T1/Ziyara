package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create a new provider staff login and link it to the current provider")
public class CreatePortalStaffUserRequest {

    @NotBlank
    @Email
    @Schema(description = "Staff email", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Initial password", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String password;

    @Schema(description = "Optional phone")
    private String phone;

    @NotNull
    @Schema(description = "Provider role for this account", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"PROVIDER_MANAGER", "PROVIDER_FINANCE", "PROVIDER_STAFF", "TAXI_OPERATOR"})
    private UserRole role;

    @Schema(description = "Optional title label in provider team")
    private String title;
}

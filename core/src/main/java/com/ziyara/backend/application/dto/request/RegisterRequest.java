package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Registration request (public — mobile app and landing page)")
public class RegisterRequest {

    @NotBlank
    @Email
    @Schema(description = "Email", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank
    @Size(min = 6)
    @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String password;

    @NotBlank
    @Schema(description = "First name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank
    @Schema(description = "Last name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Date of birth (ISO date, e.g. 1990-05-20)")
    private LocalDate dateOfBirth;

    @Schema(description = "Nationality")
    private String nationality;

    @Schema(description = "Role — always CUSTOMER for public registration; defaults to CUSTOMER when omitted")
    private UserRole role;
}

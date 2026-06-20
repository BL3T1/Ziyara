package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
@Schema(description = "Update user request")
public class UpdateUserRequest {

    @Email
    @Schema(description = "User email")
    private String email;

    @Schema(description = "Login username for company staff (unique)")
    @jakarta.validation.constraints.Size(max = 50)
    private String username;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "User status")
    private UserStatus status;

    @Schema(description = "First name")
    @jakarta.validation.constraints.Size(max = 100)
    private String firstName;

    @Schema(description = "Last name")
    @jakarta.validation.constraints.Size(max = 100)
    private String lastName;
}

package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Link an existing portal user to the current provider")
public class AddPortalStaffRequest {

    @NotNull
    @Schema(description = "Existing user id (must have a provider portal role)", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID userId;

    @Schema(description = "Optional job title label for this member")
    private String title;
}

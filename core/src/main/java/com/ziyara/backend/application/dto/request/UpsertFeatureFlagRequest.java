package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create or update a feature flag by key")
public class UpsertFeatureFlagRequest {

    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_.]{1,126}$", message = "flagKey must start with a letter and use lowercase letters, digits, dots, underscores")
    @Schema(description = "Stable flag identifier", example = "landing.soft_banner")
    private String flagKey;

    @Schema(description = "Whether the flag is on")
    private Boolean enabled;

    @Schema(description = "Human-readable description")
    private String description;
}

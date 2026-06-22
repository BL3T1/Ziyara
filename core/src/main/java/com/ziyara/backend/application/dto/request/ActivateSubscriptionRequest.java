package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to activate a subscription plan for a provider")
public class ActivateSubscriptionRequest {

    @NotBlank(message = "Plan code is required")
    @Schema(description = "Plan code to activate, e.g. STARTER, PROFESSIONAL, ENTERPRISE",
            example = "STARTER")
    private String planCode;
}

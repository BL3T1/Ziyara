package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Set or update a provider's subscription plan")
public class UpsertProviderSubscriptionRequest {

    @NotBlank
    @Schema(description = "Plan: FREE or PRO", allowableValues = {"FREE", "PRO"})
    private String plan;

    @Min(1)
    @Schema(description = "Maximum staff accounts allowed. Ignored for FREE (fixed at 10).")
    private Integer staffLimit;
}

package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Provider subscription details")
public class ProviderSubscriptionResponse {
    private UUID id;
    private UUID providerId;
    private String providerName;
    @Schema(description = "FREE or PRO")
    private String plan;
    @Schema(description = "Maximum staff accounts allowed")
    private int staffLimit;
}

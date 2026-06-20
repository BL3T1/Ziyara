package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Subscription plan details")
public class PlanResponse {

    @Schema(description = "Plan ID")
    private UUID id;

    @Schema(description = "Plan code, e.g. FREE, STARTER, PROFESSIONAL, ENTERPRISE")
    private String code;

    @Schema(description = "Display name")
    private String name;

    @Schema(description = "Plan description")
    private String description;

    @Schema(description = "Maximum portal users allowed (-1 = unlimited)")
    private int maxUsers;

    @Schema(description = "Monthly price")
    private BigDecimal monthlyPrice;

    @Schema(description = "Billing currency (ISO 4217)")
    private String currency;

    @Schema(description = "Whether per-user overage billing is supported")
    private boolean allowsOverage;

    @Schema(description = "Overage charge per extra user per month (null if not applicable)")
    private BigDecimal overagePricePerUser;
}

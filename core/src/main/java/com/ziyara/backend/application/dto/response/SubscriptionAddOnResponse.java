package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Subscription seat-expansion add-on")
public class SubscriptionAddOnResponse {

    @Schema(description = "Add-on ID")
    private UUID id;

    @Schema(description = "Add-on product code")
    private String addOnCode;

    @Schema(description = "Display name")
    private String displayName;

    @Schema(description = "Additional seats granted")
    private int extraSeats;

    @Schema(description = "Price paid")
    private BigDecimal price;

    @Schema(description = "Status: ACTIVE, CANCELLED, EXPIRED")
    private String status;

    @Schema(description = "When this add-on was activated")
    private Instant activatedAt;

    @Schema(description = "Expiry date (null = no expiry)")
    private Instant expiresAt;
}

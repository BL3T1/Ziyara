package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create a provider self-funded discount code")
public class CreatePortalDiscountRequest {

    @NotBlank
    @Schema(description = "Unique discount code (case-insensitive at validation)")
    private String code;

    @NotBlank
    @Schema(description = "PERCENTAGE or FIXED_AMOUNT")
    private String type;

    @NotNull
    @Positive
    @Schema(description = "Discount value — percentage (0–100) or fixed amount")
    private BigDecimal value;

    @Schema(description = "Short description shown to customers")
    private String description;

    @NotNull
    @Schema(description = "Expiry date/time (ISO-8601)")
    private LocalDateTime endDate;

    @Schema(description = "Max redemptions — 0 or null = unlimited")
    private Integer usageLimit;

    @Schema(description = "Minimum booking amount to qualify")
    private BigDecimal minBookingAmount;

    @Schema(description = "Cap on percentage discount (PERCENTAGE type only)")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "Restrict to these listing (service) UUIDs — empty = all provider listings")
    private List<UUID> applicableServiceIds;
}

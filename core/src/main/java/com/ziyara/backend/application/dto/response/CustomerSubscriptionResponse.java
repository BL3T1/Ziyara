package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Active subscription for a service provider")
public class CustomerSubscriptionResponse {

    @Schema(description = "Subscription ID")
    private UUID id;

    @Schema(description = "Provider ID this subscription belongs to")
    private UUID providerId;

    @Schema(description = "Plan code, e.g. FREE")
    private String planCode;

    @Schema(description = "Plan display name")
    private String planName;

    @Schema(description = "Subscription status: TRIAL, ACTIVE, PAST_DUE, CANCELLED, EXPIRED")
    private String status;

    @Schema(description = "Base seat limit from the plan")
    private int seatLimit;

    @Schema(description = "Effective seat limit including active add-ons")
    private int effectiveSeatLimit;

    @Schema(description = "Current number of active portal users")
    private int currentSeatCount;

    @Schema(description = "Remaining seats before limit is hit")
    private int seatsRemaining;

    @Schema(description = "Billing period start")
    private Instant currentPeriodStart;

    @Schema(description = "Billing period end (null for open-ended)")
    private Instant currentPeriodEnd;

    @Schema(description = "Active seat-expansion add-ons")
    private List<SubscriptionAddOnResponse> addOns;
}

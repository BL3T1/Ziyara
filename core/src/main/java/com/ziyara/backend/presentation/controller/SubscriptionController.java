package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.ActivateSubscriptionRequest;
import com.ziyara.backend.application.dto.request.AddSubscriptionAddOnRequest;
import com.ziyara.backend.application.dto.response.CustomerSubscriptionResponse;
import com.ziyara.backend.application.dto.response.PlanResponse;
import com.ziyara.backend.application.service.SubscriptionService;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller: SubscriptionController
 *
 * Manages per-provider subscription plans and seat-expansion add-ons.
 *
 * Base path: /providers/{providerId}/subscription
 *
 * Authorisation:
 *   - GET endpoints: PROVIDER_PORTAL (managers can see their own plan)
 *   - Mutation endpoints: SUPER_ADMIN / CEO only (billing operations)
 */
@RestController
@RequestMapping("/providers/{providerId}/subscription")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Provider subscription plan and seat management")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // -------------------------------------------------------------------------
    // Plan catalogue (public-ish — any authenticated staff can browse plans)
    // -------------------------------------------------------------------------

    @GetMapping("/plans")
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF + " or " + ApiAuthorizationExpressions.PROVIDER_PORTAL)
    @Operation(summary = "List available plans",
               description = "Returns the full catalogue of active subscription plans with pricing.")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> listPlans(
            @PathVariable UUID providerId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.listPlans()));
    }

    // -------------------------------------------------------------------------
    // Current subscription
    // -------------------------------------------------------------------------

    @GetMapping
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF + " or " + ApiAuthorizationExpressions.PROVIDER_PORTAL)
    @Operation(summary = "Get active subscription",
               description = "Returns the provider's current plan, seat limit, seat usage, "
                           + "and any active seat-expansion add-ons. "
                           + "If no subscription exists the FREE plan defaults (6 seats) are shown.")
    public ResponseEntity<ApiResponse<CustomerSubscriptionResponse>> getSubscription(
            @PathVariable UUID providerId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getSubscription(providerId)));
    }

    // -------------------------------------------------------------------------
    // Billing mutations (admin-only)
    // -------------------------------------------------------------------------

    @PostMapping("/activate")
    @PreAuthorize(ApiAuthorizationExpressions.DISCOUNT_APPROVE) // reuses SUPER_ADMIN / CEO gate
    @Operation(summary = "Activate a subscription plan",
               description = "Switches the provider to the specified plan. "
                           + "Any previous active subscription is cancelled first. "
                           + "The seat limit is set from the plan's max_users value.")
    public ResponseEntity<ApiResponse<CustomerSubscriptionResponse>> activateSubscription(
            @PathVariable UUID providerId,
            @Valid @RequestBody ActivateSubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Subscription activated",
                subscriptionService.activateSubscription(providerId, request)));
    }

    @PostMapping("/add-ons")
    @PreAuthorize(ApiAuthorizationExpressions.DISCOUNT_APPROVE)
    @Operation(summary = "Add seat-expansion add-on",
               description = "Attaches a paid seat-expansion block to the provider's active subscription. "
                           + "The effective seat limit is immediately increased by extraSeats. "
                           + "Requires an active (TRIAL or ACTIVE) subscription.")
    public ResponseEntity<ApiResponse<CustomerSubscriptionResponse>> addSeatExpansion(
            @PathVariable UUID providerId,
            @Valid @RequestBody AddSubscriptionAddOnRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Seat expansion add-on attached",
                subscriptionService.addSeatExpansion(providerId, request)));
    }

    @DeleteMapping("/add-ons/{addOnId}")
    @PreAuthorize(ApiAuthorizationExpressions.DISCOUNT_APPROVE)
    @Operation(summary = "Cancel a seat-expansion add-on",
               description = "Marks the add-on as CANCELLED. "
                           + "The effective seat limit drops immediately — verify no over-limit users exist first.")
    public ResponseEntity<ApiResponse<CustomerSubscriptionResponse>> cancelAddOn(
            @PathVariable UUID providerId,
            @PathVariable UUID addOnId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Add-on cancelled",
                subscriptionService.cancelAddOn(providerId, addOnId)));
    }
}

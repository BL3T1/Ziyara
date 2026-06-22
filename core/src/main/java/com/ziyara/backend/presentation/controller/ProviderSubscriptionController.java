package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.UpsertProviderSubscriptionRequest;
import com.ziyara.backend.application.dto.response.ProviderSubscriptionResponse;
import com.ziyara.backend.application.service.ProviderSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Provider Subscriptions", description = "Manage provider subscription plans and staff limits")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize(SUBSCRIPTIONS_READ)
public class ProviderSubscriptionController {

    private final ProviderSubscriptionService subscriptionService;

    @GetMapping
    @Operation(summary = "List all provider subscriptions")
    public ResponseEntity<ApiResponse<List<ProviderSubscriptionResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.listAll()));
    }

    @GetMapping("/{providerId}")
    @Operation(summary = "Get subscription for a specific provider")
    public ResponseEntity<ApiResponse<ProviderSubscriptionResponse>> get(@PathVariable UUID providerId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getByProviderId(providerId)));
    }

    @PutMapping("/{providerId}")
    @Operation(summary = "Set or update a provider's subscription plan and staff limit")
    public ResponseEntity<ApiResponse<ProviderSubscriptionResponse>> upsert(
            @PathVariable UUID providerId,
            @Valid @RequestBody UpsertProviderSubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.upsert(providerId, request)));
    }
}

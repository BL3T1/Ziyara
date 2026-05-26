package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.CreateServiceProviderRequest;
import com.ziyara.backend.application.dto.request.RejectServiceProviderRequest;
import com.ziyara.backend.application.dto.request.UpdateProviderCommissionRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceProviderRequest;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.application.dto.response.ServiceProviderResponse;
import com.ziyara.backend.application.service.ServiceProviderService;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import com.ziyara.backend.infrastructure.security.SecurityRoleUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller: ServiceProviderController
 * Handles provider registration and profiles
 */
@RestController
@RequestMapping("/providers")
@RequiredArgsConstructor
@Tag(name = "Service Providers", description = "Provider management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ServiceProviderController {
    
    private final ServiceProviderService providerService;
    
    @GetMapping
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
    @Operation(summary = "List providers (paged)", description = "Registered service providers; optional status filter")
    public ResponseEntity<ApiResponse<Page<ServiceProviderResponse>>> getAllProviders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ProviderStatus status,
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(ApiResponse.success(providerService.getProvidersPage(page, size, status, type)));
    }
    
    @GetMapping("/me")
    @PreAuthorize(ApiAuthorizationExpressions.PROVIDER_PORTAL)
    @Operation(summary = "Get current provider", description = "Retrieve the provider profile for the authenticated user (portal)")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> getCurrentProvider() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }
        return providerService.getProviderByUserId(userId)
                .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("No provider profile for this user")));
    }

    @PatchMapping("/me")
    @PreAuthorize(ApiAuthorizationExpressions.PROVIDER_PORTAL)
    @Operation(summary = "Update current provider", description = "Update the authenticated provider's profile (portal)")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> updateCurrentProvider(
            @Valid @RequestBody UpdateServiceProviderRequest request) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        return providerService.getProviderByUserId(userId)
                .map(me -> {
                    ServiceProviderResponse updated = providerService.updateProvider(me.getId(), request);
                    return ResponseEntity.ok(ApiResponse.success("Profile updated", updated));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("No provider profile for this user")));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
    @Operation(summary = "Get provider", description = "Retrieve provider details by ID")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> getProvider(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(providerService.getProvider(id)));
    }
    
    @PostMapping
    @PreAuthorize(ApiAuthorizationExpressions.PROVIDER_SUBMIT)
    @Operation(summary = "Create provider account", description = "Super Admin / CEO: active immediately. Sales: pending until Super Admin / CEO approves.")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> registerProvider(
            @Valid @RequestBody CreateServiceProviderRequest request
    ) {
        UUID actorId = getCurrentUserId();
        if (actorId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        UserRole role = SecurityRoleUtils.currentUserRole().orElse(null);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        ServiceProviderResponse response = providerService.createProvider(request, actorId, role);
        boolean salesPath = role == UserRole.SALES_MANAGER || role == UserRole.SALES_REPRESENTATIVE;
        String message = salesPath
                ? "Provider submitted for approval"
                : "Provider created and activated";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, response));
    }
    
    @PatchMapping("/{id}")
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
    @Operation(summary = "Update provider", description = "Update provider profile details")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> updateProvider(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceProviderRequest request
    ) {
        ServiceProviderResponse response = providerService.updateProvider(id, request);
        return ResponseEntity.ok(ApiResponse.success("Provider updated successfully", response));
    }

    @PatchMapping("/{id}/commission")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GENERAL_MANAGER')")
    @Operation(summary = "Set commission rate", description = "Override provider commission (audited). Super Admin and General Manager only.")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> updateCommission(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProviderCommissionRequest request
    ) {
        UUID userId = getCurrentUserId();
        ServiceProviderResponse response = providerService.updateCommissionRate(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Commission updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
    @Operation(summary = "Delete provider", description = "Soft-delete provider (Phase 3)")
    public ResponseEntity<ApiResponse<Void>> deleteProvider(@PathVariable UUID id) {
        providerService.deleteProvider(id);
        return ResponseEntity.ok(ApiResponse.success("Provider deleted", null));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(ApiAuthorizationExpressions.PROVIDER_APPROVE_OR_REJECT)
    @Operation(summary = "Approve pending provider", description = "Super Admin / CEO: activate provider and portal login")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> approveProvider(@PathVariable UUID id) {
        UUID approverId = getCurrentUserId();
        if (approverId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success("Provider approved", providerService.approveProvider(id, approverId)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(ApiAuthorizationExpressions.PROVIDER_APPROVE_OR_REJECT)
    @Operation(summary = "Reject pending provider", description = "Sets provider INACTIVE and deactivates linked PROVIDER_MANAGER")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> rejectProvider(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) RejectServiceProviderRequest body
    ) {
        UUID actorId = getCurrentUserId();
        if (actorId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        String reason = body != null ? body.getReason() : null;
        return ResponseEntity.ok(ApiResponse.success("Provider rejected", providerService.rejectProvider(id, actorId, reason)));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
    @Operation(summary = "Suspend provider", description = "Set provider status to SUSPENDED (Phase 3)")
    public ResponseEntity<ApiResponse<ServiceProviderResponse>> suspendProvider(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(providerService.suspendProvider(id)));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }
}

package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.CreatePortalSupportRequest;
import com.ziyara.backend.application.dto.response.PortalSupportRequestResponse;
import com.ziyara.backend.application.service.PortalSupportRequestService;
import com.ziyara.backend.application.service.ServiceProviderService;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Phase 5: provider portal support requests (stored for company follow-up; not internal /tickets).
 */
@RestController
@RequestMapping("/portal/support-requests")
@RequiredArgsConstructor
@PreAuthorize(ApiAuthorizationExpressions.PROVIDER_PORTAL)
@Tag(name = "Provider Portal Support", description = "Submit and list support requests for the current provider")
@SecurityRequirement(name = "bearerAuth")
public class PortalSupportRequestsController {

    private final PortalSupportRequestService portalSupportRequestService;
    private final ServiceProviderService providerService;

    @GetMapping
    @Operation(summary = "List support requests", description = "Newest first, scoped to the current provider")
    public ResponseEntity<ApiResponse<List<PortalSupportRequestResponse>>> list() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalSupportRequestService.listForProvider(providerId)));
    }

    @PostMapping
    @Operation(summary = "Submit support request", description = "Creates a row for company staff to review")
    public ResponseEntity<ApiResponse<PortalSupportRequestResponse>> create(
            @Valid @RequestBody CreatePortalSupportRequest request) {
        UUID providerId = requireCurrentProviderId();
        UUID userId = requireCurrentUserId();
        PortalSupportRequestResponse created =
                portalSupportRequestService.create(providerId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Support request submitted", created));
    }

    private UUID requireCurrentProviderId() {
        UUID userId = requireCurrentUserId();
        return providerService.getProviderByUserId(userId)
                .map(p -> p.getId())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "No provider profile for this user"));
    }

    private UUID requireCurrentUserId() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        return userId;
    }

    private UUID getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}

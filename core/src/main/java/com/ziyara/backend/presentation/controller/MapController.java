package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.DeliveryLocationResponse;
import com.ziyara.backend.application.dto.response.ProviderMapPinResponse;
import com.ziyara.backend.application.service.MapService;
import com.ziyara.backend.application.service.ServiceProviderService;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.SUBSCRIPTIONS_READ;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
@Tag(name = "Map", description = "Geographic pins for admin map and portal map views")
@SecurityRequirement(name = "bearerAuth")
public class MapController {

    private final MapService mapService;
    private final ServiceProviderService providerService;

    @GetMapping("/providers")
    @PreAuthorize(SUBSCRIPTIONS_READ)
    @Operation(summary = "All active provider locations (admin map)")
    public ResponseEntity<ApiResponse<List<ProviderMapPinResponse>>> getProviderPins(
            @RequestParam(required = false) String types) {
        List<String> typeFilter = (types != null && !types.isBlank())
                ? Arrays.asList(types.split(","))
                : List.of();
        return ResponseEntity.ok(ApiResponse.success(mapService.getProviderPins(typeFilter)));
    }

    @GetMapping("/delivery/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Latest delivery location for a booking")
    public ResponseEntity<ApiResponse<DeliveryLocationResponse>> getDeliveryLocation(@PathVariable UUID bookingId) {
        return mapService.getDeliveryLocation(bookingId)
                .map(loc -> ResponseEntity.ok(ApiResponse.success(loc)))
                .orElse(ResponseEntity.notFound().build());
    }

    private UUID requireCurrentProviderId() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        return providerService.getProviderByUserId(userId)
                .map(p -> p.getId())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("No provider profile for this user"));
    }

    private UUID getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }
}

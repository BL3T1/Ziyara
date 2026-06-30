package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.service.ProviderRestaurantService;
import com.ziyara.backend.application.service.ServiceProviderService;
import com.ziyara.backend.domain.entity.ProviderRestaurant;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/portal/restaurant")
@RequiredArgsConstructor
@PreAuthorize(ApiAuthorizationExpressions.PROVIDER_PORTAL)
@Tag(name = "Provider Restaurant", description = "Provider restaurant CRUD")
@SecurityRequirement(name = "bearerAuth")
public class ProviderRestaurantController {

    private final ProviderRestaurantService restaurantService;
    private final ServiceProviderService providerService;

    @GetMapping
    @Operation(summary = "Get restaurant for current provider")
    public ResponseEntity<ApiResponse<ProviderRestaurant>> get() {
        UUID providerId = requireCurrentProviderId();
        ProviderRestaurant restaurant = restaurantService.getByProviderId(providerId);
        if (restaurant == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No restaurant found for this provider"));
        }
        return ResponseEntity.ok(ApiResponse.success(restaurant));
    }

    @PostMapping
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_SERVICES_MANAGE)
    @Operation(summary = "Create restaurant for current provider")
    public ResponseEntity<ApiResponse<ProviderRestaurant>> create(@Valid @RequestBody RestaurantRequest request) {
        UUID providerId = requireCurrentProviderId();
        var created = restaurantService.create(providerId, request.getName(), request.getNameAr(),
                request.getDescription(), request.getLogoUrl(), request.getOpeningHours());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Restaurant created", created));
    }

    @PatchMapping
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_SERVICES_MANAGE)
    @Operation(summary = "Update restaurant for current provider")
    public ResponseEntity<ApiResponse<ProviderRestaurant>> update(@Valid @RequestBody RestaurantRequest request) {
        UUID providerId = requireCurrentProviderId();
        var updated = restaurantService.update(providerId, request.getName(), request.getNameAr(),
                request.getDescription(), request.getLogoUrl(), request.getOpeningHours());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    private UUID requireCurrentProviderId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getName());
        return providerService.getProviderByUserId(userId)
                .map(p -> p.getId())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("No provider profile"));
    }

    @Data
    public static class RestaurantRequest {
        @NotBlank
        private String name;
        private String nameAr;
        private String description;
        private String logoUrl;
        private Map<String, String> openingHours;
    }
}

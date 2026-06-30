package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.service.ProviderTypeConfigService;
import com.ziyara.backend.application.service.ServiceProviderService;
import com.ziyara.backend.domain.enums.ProviderType;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/providers")
@RequiredArgsConstructor
@PreAuthorize(ApiAuthorizationExpressions.PROVIDER_TYPE_WRITE)
@Tag(name = "Admin Provider Type", description = "Manage provider type classification")
@SecurityRequirement(name = "bearerAuth")
public class AdminProviderTypeController {

    private final ServiceProviderService providerService;
    private final ProviderTypeConfigService typeConfigService;

    @PatchMapping("/{providerId}/type")
    @Operation(summary = "Change a provider's type (HOTEL, APARTMENT, EVENT_SPACE, TOUR_OPERATOR)")
    public ResponseEntity<ApiResponse<ProviderTypeConfigService.ProviderFeatureSet>> updateType(
            @PathVariable UUID providerId,
            @Valid @RequestBody UpdateProviderTypeRequest request) {
        providerService.updateProviderType(providerId, request.getProviderType());
        return ResponseEntity.ok(ApiResponse.success("Provider type updated",
                typeConfigService.getFeaturesFor(request.getProviderType())));
    }

    @GetMapping("/{providerId}/features")
    @Operation(summary = "Get the feature set for a provider based on its type")
    public ResponseEntity<ApiResponse<ProviderTypeConfigService.ProviderFeatureSet>> getFeatures(
            @PathVariable UUID providerId) {
        ProviderType type = providerService.getProviderType(providerId);
        return ResponseEntity.ok(ApiResponse.success(typeConfigService.getFeaturesFor(type)));
    }

    @Data
    public static class UpdateProviderTypeRequest {
        @NotNull
        private ProviderType providerType;
    }
}

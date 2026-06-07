package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.UpsertFeatureFlagRequest;
import com.ziyara.backend.application.dto.response.FeatureFlagResponse;
import com.ziyara.backend.application.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/feature-flags")
@RequiredArgsConstructor
@Tag(name = "Admin Feature Flags", description = "Structured feature toggles")
@SecurityRequirement(name = "bearerAuth")
public class AdminFeatureFlagsController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    @PreAuthorize(SETTINGS_WRITE)
    @Operation(summary = "List feature flags")
    public ResponseEntity<ApiResponse<List<FeatureFlagResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(featureFlagService.listAll()));
    }

    @PutMapping
    @PreAuthorize(SETTINGS_WRITE)
    @Operation(summary = "Upsert feature flag", description = "Create or update by flagKey")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> upsert(@Valid @RequestBody UpsertFeatureFlagRequest request) {
        UUID userId = getCurrentUserId();
        FeatureFlagResponse body = featureFlagService.upsert(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Feature flag saved", body));
    }

    private static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}

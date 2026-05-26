package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.UpdateSystemSettingsRequest;
import com.ziyara.backend.application.dto.response.SystemSettingsResponse;
import com.ziyara.backend.application.service.SystemSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@Tag(name = "Admin Settings", description = "Platform settings (executive roles)")
@SecurityRequirement(name = "bearerAuth")
public class AdminSystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CEO','GENERAL_MANAGER')")
    @Operation(summary = "Get system settings", description = "Merged defaults and stored values")
    public ResponseEntity<ApiResponse<SystemSettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success(systemSettingsService.getSettings()));
    }

    @PatchMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CEO','GENERAL_MANAGER')")
    @Operation(summary = "Update system settings", description = "Partial update; omitted fields unchanged")
    public ResponseEntity<ApiResponse<SystemSettingsResponse>> patch(@Valid @RequestBody UpdateSystemSettingsRequest request) {
        UUID userId = getCurrentUserId();
        SystemSettingsResponse updated = systemSettingsService.update(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Settings saved", updated));
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

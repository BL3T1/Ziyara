package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.AddPortalStaffRequest;
import com.ziyara.backend.application.dto.request.CreatePortalStaffUserRequest;
import com.ziyara.backend.application.dto.request.ResetPortalStaffPasswordRequest;
import com.ziyara.backend.application.dto.request.UpdatePortalStaffRequest;
import com.ziyara.backend.application.dto.response.PortalAssignableRoleResponse;
import com.ziyara.backend.application.dto.response.PortalStaffMemberResponse;
import com.ziyara.backend.application.service.PortalStaffService;
import com.ziyara.backend.application.service.ServiceProviderService;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;
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

@RestController
@RequestMapping("/portal/staff")
@RequiredArgsConstructor
@PreAuthorize(PROVIDER_PORTAL)
@Tag(name = "Provider Portal Staff", description = "Team members linked to the current provider")
@SecurityRequirement(name = "bearerAuth")
public class PortalStaffController {

    private final PortalStaffService portalStaffService;
    private final ServiceProviderService providerService;

    @GetMapping
    @Operation(summary = "List staff", description = "Primary owner plus linked portal users for this provider")
    public ResponseEntity<ApiResponse<List<PortalStaffMemberResponse>>> list() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalStaffService.listStaff(providerId)));
    }

    @PostMapping
    @Operation(summary = "Add staff", description = "Link an existing provider-role user to this organization")
    public ResponseEntity<ApiResponse<PortalStaffMemberResponse>> add(@Valid @RequestBody AddPortalStaffRequest request) {
        UUID providerId = requireCurrentProviderId();
        PortalStaffMemberResponse created = portalStaffService.addStaff(providerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Staff member added", created));
    }

    @GetMapping("/roles")
    @Operation(summary = "List assignable roles", description = "Custom roles an admin has flagged as assignable to this provider's staff")
    public ResponseEntity<ApiResponse<List<PortalAssignableRoleResponse>>> listAssignableRoles() {
        return ResponseEntity.ok(ApiResponse.success(portalStaffService.listAssignableRoles()));
    }

    @PostMapping("/users")
    @Operation(summary = "Create staff user", description = "Provider Manager creates a new provider portal user and links it to this organization")
    public ResponseEntity<ApiResponse<PortalStaffMemberResponse>> createUser(
            @Valid @RequestBody CreatePortalStaffUserRequest request) {
        UUID providerId = requireCurrentProviderId();
        UUID actorUserId = getCurrentUserId();
        if (actorUserId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        PortalStaffMemberResponse created = portalStaffService.createStaffUser(providerId, actorUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Staff user created", created));
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Update staff", description = "Update title for a linked staff member")
    public ResponseEntity<ApiResponse<PortalStaffMemberResponse>> update(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdatePortalStaffRequest request) {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(portalStaffService.updateStaff(providerId, userId, request)));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Remove staff", description = "Unlink a staff user (cannot remove primary owner)")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable UUID userId) {
        UUID providerId = requireCurrentProviderId();
        portalStaffService.removeStaff(providerId, userId);
        return ResponseEntity.ok(ApiResponse.success("Staff member removed", null));
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize(PORTAL_MANAGER)
    @Operation(summary = "Reset a staff member's password (portal manager only)")
    public ResponseEntity<Void> resetStaffPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ResetPortalStaffPasswordRequest request) {
        UUID providerId = requireCurrentProviderId();
        portalStaffService.resetStaffPassword(providerId, userId, request.getNewPassword());
        return ResponseEntity.noContent().build();
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
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}

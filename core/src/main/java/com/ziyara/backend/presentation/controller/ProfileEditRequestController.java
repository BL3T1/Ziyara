package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.ProfileEditRequestResponse;
import com.ziyara.backend.application.service.ProviderProfileEditService;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/profile-edit-requests")
@RequiredArgsConstructor
@PreAuthorize(ApiAuthorizationExpressions.PROFILE_EDITS_APPROVE)
@Tag(name = "Admin Profile Edit Requests", description = "Admin review of provider profile edit requests")
@SecurityRequirement(name = "bearerAuth")
public class ProfileEditRequestController {

    private final ProviderProfileEditService editService;

    @GetMapping
    @Operation(summary = "List all pending profile edit requests")
    public ResponseEntity<ApiResponse<List<ProfileEditRequestResponse>>> listPending() {
        var result = editService.listPendingRequests().stream()
                .map(ProfileEditRequestResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{requestId}/approve")
    @Operation(summary = "Approve a profile edit request")
    public ResponseEntity<ApiResponse<ProfileEditRequestResponse>> approve(@PathVariable UUID requestId) {
        UUID reviewerId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Approved",
                ProfileEditRequestResponse.from(editService.approve(requestId, reviewerId))));
    }

    @PostMapping("/{requestId}/reject")
    @Operation(summary = "Reject a profile edit request")
    public ResponseEntity<ApiResponse<ProfileEditRequestResponse>> reject(
            @PathVariable UUID requestId,
            @RequestBody RejectRequest request) {
        UUID reviewerId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Rejected",
                ProfileEditRequestResponse.from(editService.reject(requestId, reviewerId, request.getReason()))));
    }

    private UUID getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }

    @Data
    public static class RejectRequest {
        private String reason;
    }
}

package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.service.IdentityDocumentService;
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
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
@Tag(name = "Admin Identity Verification", description = "Admin endpoints for verifying customer identity documents")
@SecurityRequirement(name = "bearerAuth")
public class AdminIdentityController {

    private final IdentityDocumentService identityDocumentService;

    @GetMapping("/identity-verifications")
    @PreAuthorize(ApiAuthorizationExpressions.CUSTOMERS_READ)
    @Operation(summary = "List customers with submitted identity documents (filter: ALL, PENDING, VERIFIED)")
    public ResponseEntity<ApiResponse<List<IdentityDocumentService.IdentityVerificationEntry>>> list(
            @RequestParam(defaultValue = "ALL") String status) {
        return ResponseEntity.ok(ApiResponse.success(identityDocumentService.listVerifications(status)));
    }

    @PostMapping("/{userId}/verify-identity")
    @PreAuthorize(ApiAuthorizationExpressions.CUSTOMERS_WRITE)
    @Operation(summary = "Approve or reject a customer's identity document")
    public ResponseEntity<ApiResponse<IdentityDocumentService.IdentityStatus>> verifyIdentity(
            @PathVariable UUID userId,
            @RequestBody VerifyRequest request) {
        UUID adminId = getCurrentUserId();
        identityDocumentService.verify(userId, request.isApproved(), adminId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(
                request.isApproved() ? "Identity verified" : "Identity rejected",
                identityDocumentService.getStatus(userId)));
    }

    private UUID getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }

    @Data
    public static class VerifyRequest {
        private boolean approved;
        private String reason;
    }
}

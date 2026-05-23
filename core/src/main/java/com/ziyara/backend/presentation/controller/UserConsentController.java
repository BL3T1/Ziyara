package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.UserConsentResponse;
import com.ziyara.backend.application.service.UserConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users/me/consents")
@RequiredArgsConstructor
@Tag(name = "Consents", description = "GDPR-style consent records for the current user")
public class UserConsentController {

    private final UserConsentService userConsentService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List consent history for current user")
    public ResponseEntity<ApiResponse<List<UserConsentResponse>>> list() {
        UUID userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success(userConsentService.list(userId)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Record a consent decision")
    public ResponseEntity<ApiResponse<UserConsentResponse>> grant(
            @Valid @RequestBody ConsentGrantBody body,
            HttpServletRequest request) {
        UUID userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        UserConsentResponse saved = userConsentService.recordGrant(
                userId, body.getConsentType(), body.getPurpose(), body.isGranted(), ip, ua);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PostMapping("/withdraw")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Withdraw a consent type")
    public ResponseEntity<ApiResponse<Void>> withdraw(@Valid @RequestBody ConsentWithdrawBody body) {
        UUID userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        try {
            userConsentService.withdraw(userId, body.getConsentType(), body.getReason());
            return ResponseEntity.ok(ApiResponse.success("Withdrawn", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private static UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Data
    public static class ConsentGrantBody {
        @NotBlank
        private String consentType;
        @NotBlank
        private String purpose;
        private boolean granted = true;
    }

    @Data
    public static class ConsentWithdrawBody {
        @NotBlank
        private String consentType;
        private String reason;
    }
}

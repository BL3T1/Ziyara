package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.service.UserMfaService;
import com.ziyara.backend.application.service.UserMfaService.MfaEnrollmentStartResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users/me/mfa")
@RequiredArgsConstructor
@Tag(name = "MFA", description = "TOTP enrollment for the current user")
public class MfaController {

    private final UserMfaService userMfaService;

    @PostMapping("/enroll/start")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Start TOTP enrollment", description = "Returns secret and otpauth URI; confirm with /enroll/confirm")
    public ResponseEntity<ApiResponse<MfaStartResponse>> start() {
        UUID userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        try {
            MfaEnrollmentStartResult r = userMfaService.startEnrollment(userId);
            return ResponseEntity.ok(ApiResponse.success(MfaStartResponse.from(r)));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/enroll/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm TOTP enrollment")
    public ResponseEntity<ApiResponse<Void>> confirm(@Valid @RequestBody MfaConfirmBody body) {
        UUID userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        try {
            userMfaService.confirmEnrollment(userId, body.getCode());
            return ResponseEntity.ok(ApiResponse.success("MFA enabled", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Disable MFA for current user")
    public ResponseEntity<ApiResponse<Void>> disable() {
        UUID userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        userMfaService.disable(userId);
        return ResponseEntity.ok(ApiResponse.success("MFA disabled", null));
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
    public static class MfaStartResponse {
        private String base32Secret;
        private String otpauthUri;

        static MfaStartResponse from(MfaEnrollmentStartResult r) {
            MfaStartResponse o = new MfaStartResponse();
            o.setBase32Secret(r.base32Secret());
            o.setOtpauthUri(r.otpauthUri());
            return o;
        }
    }

    @Data
    public static class MfaConfirmBody {
        @NotBlank
        private String code;
    }
}

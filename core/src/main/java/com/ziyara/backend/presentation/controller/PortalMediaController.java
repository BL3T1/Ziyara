package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.ProviderMediaSubmissionResponse;
import com.ziyara.backend.application.service.ProviderMediaSubmissionService;
import com.ziyara.backend.application.service.ServiceProviderService;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
@PreAuthorize(ApiAuthorizationExpressions.PROVIDER_PORTAL)
@Tag(name = "Portal Media", description = "Provider portal: submit and view media submissions")
@SecurityRequirement(name = "bearerAuth")
public class PortalMediaController {

    private final ProviderMediaSubmissionService submissionService;
    private final ServiceProviderService providerService;

    @GetMapping("/media-submissions")
    @Operation(summary = "My media submissions", description = "List all media submissions for the current provider")
    public ResponseEntity<ApiResponse<List<ProviderMediaSubmissionResponse>>> getMySubmissions() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(submissionService.getProviderSubmissions(providerId)));
    }

    @PostMapping("/logo/submit")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_MEDIA_SUBMIT)
    @Operation(summary = "Submit provider logo", description = "Upload a provider logo image for admin approval")
    public ResponseEntity<ApiResponse<ProviderMediaSubmissionResponse>> submitLogo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText) throws IOException {
        UUID providerId = requireCurrentProviderId();
        UUID userId = getCurrentUserId();
        ProviderMediaSubmissionResponse result = submissionService.submitProviderLogo(
                providerId,
                file.getBytes(),
                file.getContentType(),
                file.getOriginalFilename(),
                altText,
                userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Logo submitted for review", result));
    }

    @PostMapping("/services/{serviceId}/images/submit")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_MEDIA_SUBMIT)
    @Operation(summary = "Submit service image", description = "Upload a service image for admin approval")
    public ResponseEntity<ApiResponse<ProviderMediaSubmissionResponse>> submitServiceImage(
            @PathVariable UUID serviceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "imageType", required = false, defaultValue = "SERVICE") String imageType,
            @RequestParam(value = "primary", required = false, defaultValue = "false") boolean primary) throws IOException {
        UUID providerId = requireCurrentProviderId();
        UUID userId = getCurrentUserId();
        ProviderMediaSubmissionResponse result = submissionService.submitServiceImage(
                providerId,
                serviceId,
                file.getBytes(),
                file.getContentType(),
                file.getOriginalFilename(),
                imageType,
                altText,
                primary,
                userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Image submitted for review", result));
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
            try { return UUID.fromString(auth.getName()); } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }
}

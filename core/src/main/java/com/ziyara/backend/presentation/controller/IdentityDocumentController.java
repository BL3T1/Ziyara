package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.service.IdentityDocumentService;
import com.ziyara.backend.infrastructure.media.MediaStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Identity Document", description = "Customer identity verification")
@SecurityRequirement(name = "bearerAuth")
public class IdentityDocumentController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "application/pdf");
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10 MB

    private final IdentityDocumentService identityDocumentService;
    private final MediaStorageService mediaStorageService;

    @GetMapping("/identity-status")
    @Operation(summary = "Get current identity document status")
    public ResponseEntity<ApiResponse<IdentityDocumentService.IdentityStatus>> getStatus() {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(identityDocumentService.getStatus(userId)));
    }

    @PostMapping(value = "/identity-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload identity document (passport or national ID)")
    public ResponseEntity<ApiResponse<IdentityDocumentService.IdentityStatus>> upload(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File is empty"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Allowed file types: JPG, PNG, PDF"));
        }
        if (file.getSize() > MAX_SIZE) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File too large (max 10 MB)"));
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to read the uploaded file. Please try again."));
        }

        UUID userId = getCurrentUserId();
        String url = mediaStorageService.storeIdentityDocument(userId, bytes,
                contentType, file.getOriginalFilename());
        identityDocumentService.uploadDocument(userId, url);

        return ResponseEntity.ok(ApiResponse.success("Document uploaded — pending verification",
                identityDocumentService.getStatus(userId)));
    }

    private UUID getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}

package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.DataExportRequestResponse;
import com.ziyara.backend.application.service.DataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users/me/data-exports")
@RequiredArgsConstructor
@Tag(name = "Data export", description = "GDPR-style data portability requests")
public class UserDataExportController {

    private final DataExportService dataExportService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List prior export requests")
    public ResponseEntity<ApiResponse<List<DataExportRequestResponse>>> list() {
        UUID userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success(dataExportService.list(userId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get one export request (poll while PENDING)")
    public ResponseEntity<ApiResponse<DataExportRequestResponse>> getOne(@PathVariable("id") UUID id) {
        UUID userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.success(dataExportService.getForUser(id, userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Not found"));
        }
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download completed export JSON")
    public ResponseEntity<byte[]> download(@PathVariable("id") UUID id) {
        UUID userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            byte[] body = dataExportService.downloadPayloadForUser(id, userId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ziyara-export-" + id + ".json\"")
                    .body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).build();
        }
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Request a JSON export (async; poll GET until COMPLETED)")
    public ResponseEntity<ApiResponse<DataExportRequestResponse>> request() {
        UUID userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success(dataExportService.requestExport(userId)));
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
}

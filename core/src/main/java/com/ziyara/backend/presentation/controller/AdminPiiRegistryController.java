package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.PiiFieldRegistryResponse;
import com.ziyara.backend.application.service.PiiRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/compliance/pii-registry")
@RequiredArgsConstructor
@Tag(name = "Compliance", description = "PII field inventory (read-only)")
public class AdminPiiRegistryController {

    private final PiiRegistryService piiRegistryService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List registered PII columns")
    public ResponseEntity<ApiResponse<List<PiiFieldRegistryResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(piiRegistryService.listAll()));
    }
}

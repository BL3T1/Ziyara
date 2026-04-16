package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.UpsertContentPageRequest;
import com.ziyara.backend.application.dto.response.ContentPageResponse;
import com.ziyara.backend.application.service.ContentPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.CONTENT_PAGE_EDITOR;

@RestController
@RequestMapping("/content-pages")
@RequiredArgsConstructor
@Tag(name = "Content Pages", description = "Landing website content pages (public read, staff update)")
public class ContentPageController {

    private final ContentPageService contentPageService;

    @GetMapping("/{slug}")
    @Operation(summary = "Get public content page", description = "Public endpoint for website pages")
    public ResponseEntity<ApiResponse<ContentPageResponse>> getPublicPage(
            @PathVariable String slug,
            @RequestParam(required = false) String lang
    ) {
        return ResponseEntity.ok(ApiResponse.success(contentPageService.getPublicPage(slug, lang)));
    }

    @PutMapping("/{slug}")
    @PreAuthorize(CONTENT_PAGE_EDITOR)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Upsert content page",
            description = "Create/update page content. Allowed roles: SUPER_ADMIN, SALES_MANAGER, SALES_REPRESENTATIVE, CEO, GENERAL_MANAGER."
    )
    public ResponseEntity<ApiResponse<ContentPageResponse>> upsert(
            @PathVariable String slug,
            @Valid @RequestBody UpsertContentPageRequest request
    ) {
        ContentPageResponse response = contentPageService.upsert(slug, request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Content page saved", response));
    }
}

package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.ReviewMediaSubmissionRequest;
import com.ziyara.backend.application.dto.response.ProviderMediaSubmissionResponse;
import com.ziyara.backend.application.service.ProviderMediaSubmissionService;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/media-submissions")
@RequiredArgsConstructor
@Tag(name = "Media Submissions", description = "Admin: review and approve/reject provider image submissions")
@SecurityRequirement(name = "bearerAuth")
public class ProviderMediaSubmissionController {

    private final ProviderMediaSubmissionService submissionService;

    @GetMapping
    @PreAuthorize(MEDIA_SUBMISSIONS_APPROVE)
    @Operation(summary = "List pending media submissions")
    public ResponseEntity<ApiResponse<List<ProviderMediaSubmissionResponse>>> listPending(
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        List<ProviderMediaSubmissionResponse> result = all
                ? submissionService.getPendingSubmissions()
                : submissionService.getPendingSubmissions();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(MEDIA_SUBMISSIONS_APPROVE)
    @Operation(summary = "Approve media submission", description = "Makes the image live immediately")
    public ResponseEntity<ApiResponse<ProviderMediaSubmissionResponse>> approve(@PathVariable UUID id) {
        UUID reviewerId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Submission approved", submissionService.approve(id, reviewerId)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(MEDIA_SUBMISSIONS_APPROVE)
    @Operation(summary = "Reject media submission")
    public ResponseEntity<ApiResponse<ProviderMediaSubmissionResponse>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewMediaSubmissionRequest body) {
        UUID reviewerId = getCurrentUserId();
        String note = body != null ? body.getNote() : null;
        return ResponseEntity.ok(ApiResponse.success("Submission rejected", submissionService.reject(id, reviewerId, note)));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try { return UUID.fromString(auth.getName()); } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }
}

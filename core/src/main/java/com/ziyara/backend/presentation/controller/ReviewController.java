package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.CreateReviewRequest;
import com.ziyara.backend.application.dto.request.ModerateReviewRequest;
import com.ziyara.backend.application.dto.request.UpdateReviewRequest;
import com.ziyara.backend.application.dto.response.ReviewResponse;
import com.ziyara.backend.application.service.ReviewService;
import com.ziyara.backend.domain.enums.ReviewStatus;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import com.ziyara.backend.infrastructure.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.COMPANY_STAFF;

/**
 * Controller: ReviewController
 * Handles user feedback and ratings
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Review and rating APIs")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {
    
    private final ReviewService reviewService;
    private final JwtService jwtService;

    @GetMapping
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "List reviews (admin)", description = "Paginated list with optional status, serviceId, and created date range")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> listAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.listAdmin(page, size, status, serviceId, start, end)));
    }

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "Get service reviews", description = "Retrieve all published reviews for a service")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getServiceReviews(@PathVariable UUID serviceId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getServiceReviews(serviceId)));
    }
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit review", description = "Add a new review for a completed booking")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review", description = "Get review by ID")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReview(id)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update review", description = "Update rating or comment — author or staff only")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewRequest request) {
        UUID requestingUserId = currentUserId();
        boolean isStaff = ApiAuthorizationExpressions.isCompanyStaff(
                SecurityContextHolder.getContext().getAuthentication());
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.updateReview(id, request, requestingUserId, isStaff)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete review", description = "Delete a review — author or staff only")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable UUID id) {
        UUID requestingUserId = currentUserId();
        boolean isStaff = ApiAuthorizationExpressions.isCompanyStaff(
                SecurityContextHolder.getContext().getAuthentication());
        reviewService.deleteReview(id, requestingUserId, isStaff);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }

    @PostMapping("/{id}/moderate")
    @PreAuthorize(COMPANY_STAFF)
    @Operation(summary = "Moderate review", description = "Set status PUBLISHED/REJECTED/HIDDEN — staff only")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderateReview(
            @PathVariable UUID id,
            @Valid @RequestBody ModerateReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.moderateReview(id, request)));
    }

    @PostMapping("/{id}/respond")
    @PreAuthorize(ApiAuthorizationExpressions.PORTAL_MANAGER)
    @Operation(summary = "Respond to review", description = "Provider manager response to a user review")
    public ResponseEntity<ApiResponse<ReviewResponse>> respondToReview(
            @PathVariable UUID id,
            @RequestParam String response
    ) {
        ReviewResponse reviewResponse = reviewService.respondToReview(id, response);
        return ResponseEntity.ok(ApiResponse.success("Response added", reviewResponse));
    }

    private UUID extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        return UUID.fromString(jwtService.extractUserId(token));
    }

    private UUID currentUserId() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        return UUID.fromString(name);
    }
}

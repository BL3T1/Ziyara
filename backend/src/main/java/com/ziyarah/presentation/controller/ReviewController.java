package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.dto.request.CreateReviewRequest;
import com.ziyarah.application.dto.response.ReviewResponse;
import com.ziyarah.application.service.ReviewService;
import com.ziyarah.infrastructure.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
    
    @GetMapping("/service/{serviceId}")
    @Operation(summary = "Get service reviews", description = "Retrieve all published reviews for a service")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getServiceReviews(@PathVariable UUID serviceId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getServiceReviews(serviceId)));
    }
    
    @PostMapping
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
    
    @PostMapping("/{id}/respond")
    @Operation(summary = "Respond to review", description = "Provider response to a user review")
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
}

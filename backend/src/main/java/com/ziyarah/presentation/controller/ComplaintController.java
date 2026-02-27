package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.dto.request.CreateComplaintCommentRequest;
import com.ziyarah.application.dto.response.ComplaintCommentResponse;
import com.ziyarah.application.service.ComplaintCommentService;
import com.ziyarah.infrastructure.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller: ComplaintController
 * Handles ticket comments and support interactions
 */
@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "Support and complaint management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ComplaintController {
    
    private final ComplaintCommentService commentService;
    private final JwtService jwtService;
    
    @GetMapping("/{id}/comments")
    @Operation(summary = "Get comments", description = "Retrieve all comments for a complaint")
    public ResponseEntity<ApiResponse<List<ComplaintCommentResponse>>> getComments(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean includeInternal
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getComplaintComments(id, includeInternal)));
    }
    
    @PostMapping("/{id}/comments")
    @Operation(summary = "Add comment", description = "Add a new comment to a complaint")
    public ResponseEntity<ApiResponse<ComplaintCommentResponse>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateComplaintCommentRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        ComplaintCommentResponse response = commentService.addComment(id, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Comment added", response));
    }
    
    private UUID extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        return UUID.fromString(jwtService.extractUserId(token));
    }
}

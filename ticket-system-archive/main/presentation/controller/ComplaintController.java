package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.*;
import com.ziyara.backend.application.dto.response.ComplaintCommentResponse;
import com.ziyara.backend.application.dto.response.ComplaintResponse;
import com.ziyara.backend.application.query.ComplaintQueryHandler;
import com.ziyara.backend.application.service.ComplaintCommentService;
import com.ziyara.backend.application.service.ComplaintService;
import com.ziyara.backend.domain.enums.ComplaintPriority;
import com.ziyara.backend.domain.enums.ComplaintStatus;
import com.ziyara.backend.infrastructure.security.JwtService;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ziyara.backend.application.annotation.RateLimit;

import java.util.List;
import java.util.UUID;

/**
 * Controller: ComplaintController
 * Handles complaints full lifecycle and comments (Phase 2).
 * GET list/by-id = jOOQ query handler; POST/PUT/assign/resolve/escalate/close = ComplaintService (JPA).
 */
@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
@PreAuthorize(COMPANY_STAFF)
@Tag(name = "Complaints", description = "Support and complaint management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ComplaintController {

    private final ComplaintCommentService commentService;
    private final ComplaintService complaintService;
    private final ComplaintQueryHandler complaintQueryHandler;
    private final JwtService jwtService;

    @GetMapping
    @Operation(summary = "List complaints", description = "Paginated list with optional filters")
    public ResponseEntity<ApiResponse<Page<ComplaintResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintPriority priority,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID assignedTo) {
        Page<ComplaintResponse> result = complaintQueryHandler.findPage(page, size, status, priority, customerId, assignedTo);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get complaint", description = "Get complaint by ID")
    public ResponseEntity<ApiResponse<ComplaintResponse>> getById(@PathVariable UUID id) {
        return complaintQueryHandler.findById(id)
                .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
    }

    @RateLimit(key = "complaint-create", maxPerMinute = 10)
    @PostMapping
    @Operation(summary = "Create complaint", description = "Create a new complaint (ticket number auto-generated)")
    public ResponseEntity<ApiResponse<ComplaintResponse>> create(
            @Valid @RequestBody CreateComplaintRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        ComplaintResponse response = complaintService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Complaint created", response));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update complaint", description = "Update complaint details")
    public ResponseEntity<ApiResponse<ComplaintResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComplaintRequest request) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.update(id, request)));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign complaint", description = "Assign complaint to an agent")
    public ResponseEntity<ApiResponse<ComplaintResponse>> assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignComplaintRequest request) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.assign(id, request)));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve complaint", description = "Mark complaint as resolved")
    public ResponseEntity<ApiResponse<ComplaintResponse>> resolve(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ResolveComplaintRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(ApiResponse.success(complaintService.resolve(id, request != null ? request : ResolveComplaintRequest.builder().build(), userId)));
    }

    @PostMapping("/{id}/escalate")
    @Operation(summary = "Escalate complaint", description = "Escalate complaint to another user")
    public ResponseEntity<ApiResponse<ComplaintResponse>> escalate(
            @PathVariable UUID id,
            @Valid @RequestBody EscalateComplaintRequest request) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.escalate(id, request)));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close complaint", description = "Close a resolved or rejected complaint")
    public ResponseEntity<ApiResponse<ComplaintResponse>> close(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.close(id)));
    }

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

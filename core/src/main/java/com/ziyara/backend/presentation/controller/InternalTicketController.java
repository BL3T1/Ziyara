package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.TicketCommentRequest;
import com.ziyara.backend.application.dto.TicketRequest;
import com.ziyara.backend.application.dto.TicketResponse;
import com.ziyara.backend.application.dto.response.TicketCommentResponse;
import com.ziyara.backend.application.service.InternalTicketService;
import com.ziyara.backend.domain.enums.TicketPriority;
import com.ziyara.backend.domain.enums.TicketStatus;
import com.ziyara.backend.domain.enums.TicketType;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Internal Ticket Management.
 * All business logic and data access is delegated to {@link InternalTicketService}.
 */
@RestController
@RequestMapping("/tickets")
@PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
@Tag(name = "Internal Tickets", description = "Internal ticket management for bug reports and feature requests")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class InternalTicketController {

    private final InternalTicketService ticketService;

    // ── CRUD ──────────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new internal ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody TicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ticket created successfully",
                        ticketService.createTicket(request, userId)));
    }

    @GetMapping
    @Operation(summary = "List all internal tickets")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> listTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketType type,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) UUID assignedToId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.listTickets(status, type, priority, module, assignedToId, search)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket by ID or ticket number")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicket(id)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody TicketRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ticket updated successfully",
                ticketService.updateTicket(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ticket")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable UUID id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.ok(ApiResponse.<Void>success("Ticket deleted successfully", null));
    }

    // ── Workflow ──────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> acknowledgeTicket(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Ticket acknowledged", ticketService.acknowledge(id)));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign ticket to a user")
    public ResponseEntity<ApiResponse<TicketResponse>> assignTicket(
            @PathVariable UUID id, @RequestParam UUID assigneeId) {
        return ResponseEntity.ok(ApiResponse.success("Ticket assigned successfully",
                ticketService.assign(id, assigneeId)));
    }

    @PostMapping("/{id}/start-progress")
    @Operation(summary = "Start progress on ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> startProgress(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Work started on ticket",
                ticketService.startProgress(id)));
    }

    @PostMapping("/{id}/request-info")
    @Operation(summary = "Request information")
    public ResponseEntity<ApiResponse<TicketResponse>> requestInfo(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Information requested",
                ticketService.requestInfo(id)));
    }

    @PostMapping("/{id}/testing")
    @Operation(summary = "Move to testing")
    public ResponseEntity<ApiResponse<TicketResponse>> moveToTesting(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Ticket moved to testing",
                ticketService.moveToTesting(id)));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> resolveTicket(
            @PathVariable UUID id,
            @RequestParam String notes,
            @RequestParam(required = false) String summary,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Ticket resolved",
                ticketService.resolve(id, resolveUserId(userDetails), notes, summary)));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify resolution")
    public ResponseEntity<ApiResponse<TicketResponse>> verifyTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Ticket verified",
                ticketService.verify(id, resolveUserId(userDetails))));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> closeTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Ticket closed",
                ticketService.close(id, resolveUserId(userDetails))));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reopen a closed ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> reopenTicket(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Ticket reopened", ticketService.reopen(id)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> cancelTicket(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Ticket cancelled",
                ticketService.cancel(id, resolveUserId(userDetails), reason)));
    }

    // ── Comments ──────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/comments")
    @Operation(summary = "Get ticket comments")
    public ResponseEntity<ApiResponse<List<TicketCommentResponse>>> getComments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.listComments(id)));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a ticket")
    public ResponseEntity<ApiResponse<TicketCommentResponse>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody TicketCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully",
                        ticketService.addComment(id, request, resolveUserId(userDetails))));
    }

    // ── Statistics ────────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    @Operation(summary = "Get ticket statistics")
    public ResponseEntity<ApiResponse<InternalTicketService.TicketStats>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getStatistics()));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue tickets")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getOverdueTickets() {
        return ResponseEntity.ok(ApiResponse.success(ticketService.listOverdue()));
    }

    // ── Helper ────────────────────────────────────────────────────────────────────

    private static UUID resolveUserId(UserDetails userDetails) {
        if (userDetails == null
                || userDetails.getUsername() == null
                || userDetails.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            return UUID.fromString(userDetails.getUsername());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user context");
        }
    }
}

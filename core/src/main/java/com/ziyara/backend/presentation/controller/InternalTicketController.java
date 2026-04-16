package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.TicketCommentRequest;
import com.ziyara.backend.application.dto.TicketRequest;
import com.ziyara.backend.application.dto.TicketResponse;
import com.ziyara.backend.domain.entity.InternalTicket;
import com.ziyara.backend.domain.entity.TicketComment;
import com.ziyara.backend.domain.enums.TicketPriority;
import com.ziyara.backend.domain.enums.TicketStatus;
import com.ziyara.backend.domain.enums.TicketType;
import com.ziyara.backend.domain.repository.InternalTicketRepository;
import com.ziyara.backend.domain.repository.TicketCommentRepository;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Internal Ticket Management
 * Handles bug reports, feature requests, and system issues
 */
@RestController
@RequestMapping("/tickets")
@PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
@Tag(name = "Internal Tickets", description = "Internal ticket management for bug reports and feature requests")
@SecurityRequirement(name = "bearerAuth")
public class InternalTicketController {

    private final InternalTicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;

    public InternalTicketController(InternalTicketRepository ticketRepository,
            TicketCommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
    }

    // ==================== TICKET CRUD ====================

    @PostMapping
    @Operation(summary = "Create a new internal ticket", description = "Submit a bug report, feature request, or system issue")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody TicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        InternalTicket ticket = new InternalTicket();
        ticket.setReporterId(requireCurrentUserId(userDetails));
        ticket.setType(request.getType() != null ? request.getType() : TicketType.GENERAL_INQUIRY);
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM);
        ticket.setModule(request.getModule());
        ticket.setSubModule(request.getSubModule());
        ticket.setEnvironment(request.getEnvironment());
        ticket.setBrowser(request.getBrowser());
        ticket.setOperatingSystem(request.getOperatingSystem());
        ticket.setStepsToReproduce(request.getStepsToReproduce());
        ticket.setExpectedBehavior(request.getExpectedBehavior());
        ticket.setActualBehavior(request.getActualBehavior());
        ticket.setAttachments(request.getAttachments());
        ticket.setAssignedToId(request.getAssignedToId());
        ticket.setEstimatedHours(request.getEstimatedHours());
        ticket.setDueDate(request.getDueDate());
        ticket.setRelatedTicketId(request.getRelatedTicketId());

        // Generate ticket number
        String ticketNumber = generateTicketNumber();
        ticket.setTicketNumber(ticketNumber);

        InternalTicket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ticket created successfully", toResponse(savedTicket)));
    }

    @GetMapping
    @Operation(summary = "List all internal tickets", description = "Get all tickets with optional filtering")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> listTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketType type,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) UUID assignedToId,
            @RequestParam(required = false) String search) {

        List<InternalTicket> tickets;

        if (search != null && !search.isEmpty()) {
            tickets = ticketRepository.search(search);
        } else if (status != null) {
            tickets = ticketRepository.findByStatus(status);
        } else if (type != null) {
            tickets = ticketRepository.findByType(type);
        } else if (priority != null) {
            tickets = ticketRepository.findByPriority(priority);
        } else if (module != null) {
            tickets = ticketRepository.findByModule(module);
        } else if (assignedToId != null) {
            tickets = ticketRepository.findByAssignedToId(assignedToId);
        } else {
            tickets = ticketRepository.findAll();
        }

        List<TicketResponse> responses = tickets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket by ID", description = "Retrieve a specific ticket by its ID or ticket number")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable String id) {
        InternalTicket ticket;

        // Try to parse as UUID first, otherwise treat as ticket number
        try {
            UUID uuid = UUID.fromString(id);
            ticket = ticketRepository.findById(uuid)
                    .orElseThrow(() -> new RuntimeException("Ticket not found with ID: " + id));
        } catch (IllegalArgumentException e) {
            ticket = ticketRepository.findByTicketNumber(id)
                    .orElseThrow(() -> new RuntimeException("Ticket not found with number: " + id));
        }

        return ResponseEntity.ok(ApiResponse.success(toResponse(ticket)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ticket", description = "Update ticket details")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody TicketRequest request) {

        InternalTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with ID: " + id));

        if (request.getType() != null)
            ticket.setType(request.getType());
        if (request.getSubject() != null)
            ticket.setSubject(request.getSubject());
        if (request.getDescription() != null)
            ticket.setDescription(request.getDescription());
        if (request.getPriority() != null)
            ticket.setPriority(request.getPriority());
        if (request.getModule() != null)
            ticket.setModule(request.getModule());
        if (request.getSubModule() != null)
            ticket.setSubModule(request.getSubModule());
        if (request.getEnvironment() != null)
            ticket.setEnvironment(request.getEnvironment());
        if (request.getBrowser() != null)
            ticket.setBrowser(request.getBrowser());
        if (request.getOperatingSystem() != null)
            ticket.setOperatingSystem(request.getOperatingSystem());
        if (request.getStepsToReproduce() != null)
            ticket.setStepsToReproduce(request.getStepsToReproduce());
        if (request.getExpectedBehavior() != null)
            ticket.setExpectedBehavior(request.getExpectedBehavior());
        if (request.getActualBehavior() != null)
            ticket.setActualBehavior(request.getActualBehavior());
        if (request.getAttachments() != null)
            ticket.setAttachments(request.getAttachments());
        if (request.getEstimatedHours() != null)
            ticket.setEstimatedHours(request.getEstimatedHours());
        if (request.getDueDate() != null)
            ticket.setDueDate(request.getDueDate());
        if (request.getRelatedTicketId() != null)
            ticket.setRelatedTicketId(request.getRelatedTicketId());

        InternalTicket updatedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Ticket updated successfully", toResponse(updatedTicket)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ticket", description = "Delete a ticket (soft delete recommended)")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable UUID id) {
        ticketRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.<Void>success("Ticket deleted successfully", null));
    }

    // ==================== TICKET WORKFLOW ====================

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge ticket", description = "Mark ticket as acknowledged")
    public ResponseEntity<ApiResponse<TicketResponse>> acknowledgeTicket(@PathVariable UUID id) {
        InternalTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.acknowledge();
        InternalTicket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Ticket acknowledged", toResponse(savedTicket)));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign ticket", description = "Assign ticket to a user")
    public ResponseEntity<ApiResponse<TicketResponse>> assignTicket(
            @PathVariable UUID id,
            @RequestParam UUID assigneeId) {

        InternalTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.assign(assigneeId);
        InternalTicket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Ticket assigned successfully", toResponse(savedTicket)));
    }

    @PostMapping("/{id}/start-progress")
    @Operation(summary = "Start progress", description = "Mark ticket as in progress")
    public ResponseEntity<ApiResponse<TicketResponse>> startProgress(@PathVariable UUID id) {
        InternalTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.startProgress();
        InternalTicket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Work started on ticket", toResponse(savedTicket)));
    }

    @PostMapping("/{id}/request-info")
    @Operation(summary = "Request information", description = "Mark ticket as pending information")
    public ResponseEntity<ApiResponse<TicketResponse>> requestInfo(@PathVariable UUID id) {
        InternalTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.requestInfo();
        InternalTicket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Information requested", toResponse(savedTicket)));
    }

    @PostMapping("/{id}/testing")
    @Operation(summary = "Move to testing", description = "Mark ticket as in testing")
    public ResponseEntity<ApiResponse<TicketResponse>> moveToTesting(@PathVariable UUID id) {
        InternalTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.moveToTesting();
        InternalTicket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Ticket moved to testing", toResponse(savedTicket)));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve ticket", description = "Mark ticket as resolved")
    public ResponseEntity<ApiResponse<TicketResponse>> resolveTicket(
            @PathVariable UUID id,
            @RequestParam String notes,
            @RequestParam(required = false) String summary,
            @AuthenticationPrincipal UserDetails userDetails) {

        InternalTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        UUID resolvedBy = requireCurrentUserId(userDetails);
        ticket.resolve(resolvedBy, notes, summary);
        InternalTicket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Ticket resolved", toResponse(savedTicket)));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify resolution", description = "Verify the ticket resolution")
    public ResponseEntity<ApiResponse<TicketResponse>> verifyTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        InternalTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        UUID verifiedBy = requireCurrentUserId(userDetails);
        ticket.verify(verifiedBy);
        InternalTicket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Ticket verified", toResponse(savedTicket)));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close ticket", description = "Close the ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> closeTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        InternalTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        UUID closedBy = requireCurrentUserId(userDetails);
        ticket.close(closedBy);
        InternalTicket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Ticket closed", toResponse(savedTicket)));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reopen ticket", description = "Reopen a closed ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> reopenTicket(@PathVariable UUID id) {
        InternalTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.reopen();
        InternalTicket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Ticket reopened", toResponse(savedTicket)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel ticket", description = "Cancel the ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> cancelTicket(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails) {

        InternalTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        UUID cancelledBy = requireCurrentUserId(userDetails);
        ticket.cancel(cancelledBy, reason);
        InternalTicket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Ticket cancelled", toResponse(savedTicket)));
    }

    // ==================== COMMENTS ====================

    @GetMapping("/{id}/comments")
    @Operation(summary = "Get ticket comments", description = "Retrieve all comments for a ticket")
    public ResponseEntity<ApiResponse<List<TicketComment>>> getComments(@PathVariable UUID id) {
        List<TicketComment> comments = commentRepository.findByTicketIdOrderByCreatedAtDesc(id);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add comment", description = "Add a comment to a ticket")
    public ResponseEntity<ApiResponse<TicketComment>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody TicketCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Verify ticket exists
        ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        TicketComment comment = new TicketComment();
        comment.setTicketId(id);
        comment.setUserId(requireCurrentUserId(userDetails));
        comment.setComment(request.getComment());
        comment.setInternal(request.isInternal());
        comment.setResolution(request.isResolution());
        comment.setAttachments(request.getAttachments());

        TicketComment savedComment = commentRepository.save(comment);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully", savedComment));
    }

    // ==================== STATISTICS ====================

    @GetMapping("/stats")
    @Operation(summary = "Get ticket statistics", description = "Get ticket counts by status, type, etc.")
    public ResponseEntity<ApiResponse<TicketStats>> getStatistics() {
        TicketStats stats = new TicketStats();

        stats.setTotal(ticketRepository.count());
        stats.setOpen(ticketRepository.countByStatusIn(
                Arrays.asList(TicketStatus.SUBMITTED, TicketStatus.ACKNOWLEDGED,
                        TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS,
                        TicketStatus.PENDING_INFO, TicketStatus.TESTING)));
        stats.setResolved(ticketRepository.countByStatus(TicketStatus.RESOLVED));
        stats.setClosed(ticketRepository.countByStatus(TicketStatus.CLOSED));
        stats.setOverdue(ticketRepository.countOverdueTickets(LocalDateTime.now()));

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue tickets", description = "Get all tickets past their due date")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getOverdueTickets() {
        List<InternalTicket> tickets = ticketRepository.findOverdueTickets(LocalDateTime.now());

        List<TicketResponse> responses = tickets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // ==================== HELPER METHODS ====================

    private static UUID requireCurrentUserId(UserDetails userDetails) {
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

    private String generateTicketNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "ITK" + timestamp + random;
    }

    private TicketResponse toResponse(InternalTicket ticket) {
        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setTicketNumber(ticket.getTicketNumber());
        response.setReporterId(ticket.getReporterId());
        response.setType(ticket.getType());
        response.setSubject(ticket.getSubject());
        response.setDescription(ticket.getDescription());
        response.setPriority(ticket.getPriority());
        response.setStatus(ticket.getStatus());
        response.setModule(ticket.getModule());
        response.setSubModule(ticket.getSubModule());
        response.setEnvironment(ticket.getEnvironment());
        response.setBrowser(ticket.getBrowser());
        response.setOperatingSystem(ticket.getOperatingSystem());
        response.setStepsToReproduce(ticket.getStepsToReproduce());
        response.setExpectedBehavior(ticket.getExpectedBehavior());
        response.setActualBehavior(ticket.getActualBehavior());
        response.setAttachments(ticket.getAttachments());
        response.setAssignedToId(ticket.getAssignedToId());
        response.setAssignedAt(ticket.getAssignedAt());
        response.setEstimatedHours(ticket.getEstimatedHours());
        response.setActualHours(ticket.getActualHours());
        response.setDueDate(ticket.getDueDate());
        response.setResolutionNotes(ticket.getResolutionNotes());
        response.setResolutionSummary(ticket.getResolutionSummary());
        response.setResolvedAt(ticket.getResolvedAt());
        response.setResolvedBy(ticket.getResolvedBy());
        response.setVerifiedAt(ticket.getVerifiedAt());
        response.setVerifiedBy(ticket.getVerifiedBy());
        response.setClosedAt(ticket.getClosedAt());
        response.setClosedBy(ticket.getClosedBy());
        response.setCancelledAt(ticket.getCancelledAt());
        response.setCancelledBy(ticket.getCancelledBy());
        response.setCancellationReason(ticket.getCancellationReason());
        response.setRelatedTicketId(ticket.getRelatedTicketId());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());

        return response;
    }

    /**
     * DTO for ticket statistics
     */
    public static class TicketStats {
        private long total;
        private long open;
        private long resolved;
        private long closed;
        private long overdue;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getOpen() {
            return open;
        }

        public void setOpen(long open) {
            this.open = open;
        }

        public long getResolved() {
            return resolved;
        }

        public void setResolved(long resolved) {
            this.resolved = resolved;
        }

        public long getClosed() {
            return closed;
        }

        public void setClosed(long closed) {
            this.closed = closed;
        }

        public long getOverdue() {
            return overdue;
        }

        public void setOverdue(long overdue) {
            this.overdue = overdue;
        }
    }
}

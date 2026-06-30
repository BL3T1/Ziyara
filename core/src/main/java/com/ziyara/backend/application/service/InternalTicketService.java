package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.TicketCommentRequest;
import com.ziyara.backend.application.dto.TicketRequest;
import com.ziyara.backend.application.dto.TicketResponse;
import com.ziyara.backend.application.dto.response.TicketCommentResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.InternalTicket;
import com.ziyara.backend.domain.entity.TicketComment;
import com.ziyara.backend.domain.enums.TicketPriority;
import com.ziyara.backend.domain.enums.TicketStatus;
import com.ziyara.backend.domain.enums.TicketType;
import com.ziyara.backend.domain.repository.InternalTicketRepository;
import com.ziyara.backend.domain.repository.TicketCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for Internal Ticket management.
 * Extracted from {@code InternalTicketController} to keep domain-repository access
 * in the application layer and presentation controllers free of business logic.
 */
@Service
@RequiredArgsConstructor
public class InternalTicketService {

    private final InternalTicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;

    // ── CRUD ────────────────────────────────────────────────────────────────────

    @Transactional
    public TicketResponse createTicket(TicketRequest request, UUID reporterId) {
        InternalTicket ticket = new InternalTicket();
        ticket.setReporterId(reporterId);
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
        ticket.setTicketNumber(InternalTicket.generateTicketNumber());
        return toResponse(ticketRepository.save(ticket));
    }

    public List<TicketResponse> listTickets(
            TicketStatus status, TicketType type, TicketPriority priority,
            String module, UUID assignedToId, String search) {

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
        return tickets.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TicketResponse getTicket(String id) {
        InternalTicket ticket;
        try {
            UUID uuid = UUID.fromString(id);
            ticket = ticketRepository.findById(uuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + id));
        } catch (IllegalArgumentException e) {
            ticket = ticketRepository.findByTicketNumber(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with number: " + id));
        }
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateTicket(UUID id, TicketRequest request) {
        InternalTicket ticket = requireTicket(id);
        if (request.getType() != null)            ticket.setType(request.getType());
        if (request.getSubject() != null)          ticket.setSubject(request.getSubject());
        if (request.getDescription() != null)      ticket.setDescription(request.getDescription());
        if (request.getPriority() != null)         ticket.setPriority(request.getPriority());
        if (request.getModule() != null)           ticket.setModule(request.getModule());
        if (request.getSubModule() != null)        ticket.setSubModule(request.getSubModule());
        if (request.getEnvironment() != null)      ticket.setEnvironment(request.getEnvironment());
        if (request.getBrowser() != null)          ticket.setBrowser(request.getBrowser());
        if (request.getOperatingSystem() != null)  ticket.setOperatingSystem(request.getOperatingSystem());
        if (request.getStepsToReproduce() != null) ticket.setStepsToReproduce(request.getStepsToReproduce());
        if (request.getExpectedBehavior() != null) ticket.setExpectedBehavior(request.getExpectedBehavior());
        if (request.getActualBehavior() != null)   ticket.setActualBehavior(request.getActualBehavior());
        if (request.getAttachments() != null)      ticket.setAttachments(request.getAttachments());
        if (request.getEstimatedHours() != null)   ticket.setEstimatedHours(request.getEstimatedHours());
        if (request.getDueDate() != null)          ticket.setDueDate(request.getDueDate());
        if (request.getRelatedTicketId() != null)  ticket.setRelatedTicketId(request.getRelatedTicketId());
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public void deleteTicket(UUID id) {
        ticketRepository.deleteById(id);
    }

    // ── Workflow ─────────────────────────────────────────────────────────────────

    @Transactional public TicketResponse acknowledge(UUID id) { InternalTicket t = requireTicket(id); t.acknowledge();      return toResponse(ticketRepository.save(t)); }
    @Transactional public TicketResponse assign(UUID id, UUID assigneeId) { InternalTicket t = requireTicket(id); t.assign(assigneeId); return toResponse(ticketRepository.save(t)); }
    @Transactional public TicketResponse startProgress(UUID id) { InternalTicket t = requireTicket(id); t.startProgress(); return toResponse(ticketRepository.save(t)); }
    @Transactional public TicketResponse requestInfo(UUID id) { InternalTicket t = requireTicket(id); t.requestInfo();   return toResponse(ticketRepository.save(t)); }
    @Transactional public TicketResponse moveToTesting(UUID id) { InternalTicket t = requireTicket(id); t.moveToTesting(); return toResponse(ticketRepository.save(t)); }
    @Transactional public TicketResponse reopen(UUID id) { InternalTicket t = requireTicket(id); t.reopen();        return toResponse(ticketRepository.save(t)); }

    @Transactional
    public TicketResponse resolve(UUID id, UUID resolvedBy, String notes, String summary) {
        InternalTicket t = requireTicket(id);
        t.resolve(resolvedBy, notes, summary);
        return toResponse(ticketRepository.save(t));
    }

    @Transactional
    public TicketResponse verify(UUID id, UUID verifiedBy) {
        InternalTicket t = requireTicket(id);
        t.verify(verifiedBy);
        return toResponse(ticketRepository.save(t));
    }

    @Transactional
    public TicketResponse close(UUID id, UUID closedBy) {
        InternalTicket t = requireTicket(id);
        t.close(closedBy);
        return toResponse(ticketRepository.save(t));
    }

    @Transactional
    public TicketResponse cancel(UUID id, UUID cancelledBy, String reason) {
        InternalTicket t = requireTicket(id);
        t.cancel(cancelledBy, reason);
        return toResponse(ticketRepository.save(t));
    }

    // ── Comments ─────────────────────────────────────────────────────────────────

    public List<TicketCommentResponse> listComments(UUID ticketId) {
        return commentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId).stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TicketCommentResponse addComment(UUID ticketId, TicketCommentRequest request, UUID userId) {
        requireTicket(ticketId); // existence check
        TicketComment comment = new TicketComment();
        comment.setTicketId(ticketId);
        comment.setUserId(userId);
        comment.setComment(request.getComment());
        comment.setInternal(request.isInternal());
        comment.setResolution(request.isResolution());
        comment.setAttachments(request.getAttachments());
        return toCommentResponse(commentRepository.save(comment));
    }

    private TicketCommentResponse toCommentResponse(TicketComment c) {
        return TicketCommentResponse.builder()
                .id(c.getId())
                .ticketId(c.getTicketId())
                .userId(c.getUserId())
                .comment(c.getComment())
                .internal(c.isInternal())
                .resolution(c.isResolution())
                .attachments(c.getAttachments())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    // ── Statistics ───────────────────────────────────────────────────────────────

    public TicketStats getStatistics() {
        TicketStats stats = new TicketStats();
        stats.setTotal(ticketRepository.count());
        stats.setOpen(ticketRepository.countByStatusIn(
                Arrays.asList(TicketStatus.SUBMITTED, TicketStatus.ACKNOWLEDGED,
                        TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS,
                        TicketStatus.PENDING_INFO, TicketStatus.TESTING)));
        stats.setResolved(ticketRepository.countByStatus(TicketStatus.RESOLVED));
        stats.setClosed(ticketRepository.countByStatus(TicketStatus.CLOSED));
        stats.setOverdue(ticketRepository.countOverdueTickets(LocalDateTime.now()));
        return stats;
    }

    public List<TicketResponse> listOverdue() {
        return ticketRepository.findOverdueTickets(LocalDateTime.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private InternalTicket requireTicket(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));
    }

    public TicketResponse toResponse(InternalTicket ticket) {
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
     * DTO for ticket statistics — kept here to avoid creating a separate file for a trivial struct.
     */
    public static class TicketStats {
        private long total, open, resolved, closed, overdue;

        public long getTotal()    { return total;    }
        public long getOpen()     { return open;     }
        public long getResolved() { return resolved; }
        public long getClosed()   { return closed;   }
        public long getOverdue()  { return overdue;  }

        public void setTotal(long v)    { total    = v; }
        public void setOpen(long v)     { open     = v; }
        public void setResolved(long v) { resolved = v; }
        public void setClosed(long v)   { closed   = v; }
        public void setOverdue(long v)  { overdue  = v; }
    }
}

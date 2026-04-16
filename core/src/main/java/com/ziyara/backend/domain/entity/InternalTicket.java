package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.TicketPriority;
import com.ziyara.backend.domain.enums.TicketStatus;
import com.ziyara.backend.domain.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: InternalTicket
 * Internal tickets for bug reports, feature requests, and system issues
 * No framework dependencies - pure Java
 */
public class InternalTicket {
    
    private UUID id;
    private String ticketNumber;
    private UUID reporterId;
    private TicketType type;
    private String subject;
    private String description;
    private TicketPriority priority;
    private TicketStatus status;
    private String module;
    private String subModule;
    private String environment;
    private String browser;
    private String operatingSystem;
    private String stepsToReproduce;
    private String expectedBehavior;
    private String actualBehavior;
    private String attachments;
    private UUID assignedToId;
    private LocalDateTime assignedAt;
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private LocalDateTime dueDate;
    private String resolutionNotes;
    private String resolutionSummary;
    private LocalDateTime resolvedAt;
    private UUID resolvedBy;
    private LocalDateTime verifiedAt;
    private UUID verifiedBy;
    private LocalDateTime closedAt;
    private UUID closedBy;
    private LocalDateTime cancelledAt;
    private UUID cancelledBy;
    private String cancellationReason;
    private UUID relatedTicketId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Domain behavior
    public boolean isOpen() {
        return status.isOpen();
    }

    public void acknowledge() {
        if (status == TicketStatus.SUBMITTED) {
            this.status = TicketStatus.ACKNOWLEDGED;
        }
    }

    public void assign(UUID assigneeId) {
        if (status.canBeAssigned()) {
            this.assignedToId = assigneeId;
            this.assignedAt = LocalDateTime.now();
            this.status = TicketStatus.ASSIGNED;
        }
    }

    public void startProgress() {
        if (status == TicketStatus.ASSIGNED) {
            this.status = TicketStatus.IN_PROGRESS;
        }
    }

    public void requestInfo() {
        if (status == TicketStatus.IN_PROGRESS) {
            this.status = TicketStatus.PENDING_INFO;
        }
    }

    public void provideInfo() {
        if (status == TicketStatus.PENDING_INFO) {
            this.status = TicketStatus.IN_PROGRESS;
        }
    }

    public void moveToTesting() {
        if (status == TicketStatus.IN_PROGRESS) {
            this.status = TicketStatus.TESTING;
        }
    }

    public void resolve(UUID resolvedBy, String notes, String summary) {
        if (status.canBeResolved()) {
            this.resolvedBy = resolvedBy;
            this.resolutionNotes = notes;
            this.resolutionSummary = summary;
            this.resolvedAt = LocalDateTime.now();
            this.status = TicketStatus.RESOLVED;
        }
    }

    public void verify(UUID verifiedBy) {
        if (status == TicketStatus.RESOLVED) {
            this.verifiedBy = verifiedBy;
            this.verifiedAt = LocalDateTime.now();
            this.status = TicketStatus.VERIFIED;
        }
    }

    public void close(UUID closedBy) {
        if (status.canBeClosed()) {
            this.closedBy = closedBy;
            this.closedAt = LocalDateTime.now();
            this.status = TicketStatus.CLOSED;
        }
    }

    public void reopen() {
        if (status.canBeReopened()) {
            this.status = TicketStatus.REOPENED;
        }
    }

    public void cancel(UUID cancelledBy, String reason) {
        if (status.canBeCancelled()) {
            this.cancelledBy = cancelledBy;
            this.cancellationReason = reason;
            this.cancelledAt = LocalDateTime.now();
            this.status = TicketStatus.CANCELLED;
        }
    }

    public void updatePriority(TicketPriority newPriority) {
        this.priority = newPriority;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public void setEstimatedHours(BigDecimal hours) {
        this.estimatedHours = hours;
    }

    public void setActualHours(BigDecimal hours) {
        this.actualHours = hours;
    }

    // Constructors
    public InternalTicket() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = TicketStatus.SUBMITTED;
        this.priority = TicketPriority.MEDIUM;
        this.type = TicketType.GENERAL_INQUIRY;
    }

    public InternalTicket(UUID reporterId, TicketType type, String subject, String description) {
        this();
        this.reporterId = reporterId;
        this.type = type;
        this.subject = subject;
        this.description = description;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public UUID getReporterId() { return reporterId; }
    public void setReporterId(UUID reporterId) { this.reporterId = reporterId; }
    public TicketType getType() { return type; }
    public void setType(TicketType type) { this.type = type; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getSubModule() { return subModule; }
    public void setSubModule(String subModule) { this.subModule = subModule; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
    public String getStepsToReproduce() { return stepsToReproduce; }
    public void setStepsToReproduce(String stepsToReproduce) { this.stepsToReproduce = stepsToReproduce; }
    public String getExpectedBehavior() { return expectedBehavior; }
    public void setExpectedBehavior(String expectedBehavior) { this.expectedBehavior = expectedBehavior; }
    public String getActualBehavior() { return actualBehavior; }
    public void setActualBehavior(String actualBehavior) { this.actualBehavior = actualBehavior; }
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
    public UUID getAssignedToId() { return assignedToId; }
    public void setAssignedToId(UUID assignedToId) { this.assignedToId = assignedToId; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public BigDecimal getActualHours() { return actualHours; }
    public LocalDateTime getDueDate() { return dueDate; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public UUID getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public UUID getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(UUID verifiedBy) { this.verifiedBy = verifiedBy; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public UUID getClosedBy() { return closedBy; }
    public void setClosedBy(UUID closedBy) { this.closedBy = closedBy; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public UUID getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(UUID cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public UUID getRelatedTicketId() { return relatedTicketId; }
    public void setRelatedTicketId(UUID relatedTicketId) { this.relatedTicketId = relatedTicketId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

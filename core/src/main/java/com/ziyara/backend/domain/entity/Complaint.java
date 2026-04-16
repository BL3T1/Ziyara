package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.ComplaintPriority;
import com.ziyara.backend.domain.enums.ComplaintStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: Complaint
 * Customer complaint tickets
 * No framework dependencies - pure Java
 */
public class Complaint {
    
    private UUID id;
    private String ticketNumber;
    private UUID customerId;
    private UUID bookingId;
    private String subject;
    private String description;
    private ComplaintPriority priority;
    private ComplaintStatus status;
    private String category;
    private UUID assignedAgentId;
    private LocalDateTime assignedAt;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;
    private UUID resolvedBy;
    private LocalDateTime escalatedAt;
    private UUID escalatedTo;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Domain behavior
    public boolean isOpen() {
        return status.isOpen();
    }

    public void acknowledge() {
        if (status == ComplaintStatus.SUBMITTED) {
            this.status = ComplaintStatus.ACKNOWLEDGED;
        }
    }

    public void assign(UUID agentId) {
        if (status.canBeAssigned()) {
            this.assignedAgentId = agentId;
            this.assignedAt = LocalDateTime.now();
            this.status = ComplaintStatus.ASSIGNED;
        }
    }

    public void startProgress() {
        if (status == ComplaintStatus.ASSIGNED) {
            this.status = ComplaintStatus.IN_PROGRESS;
        }
    }

    public void requestInfo() {
        if (status == ComplaintStatus.IN_PROGRESS) {
            this.status = ComplaintStatus.PENDING_INFO;
        }
    }

    public void provideInfo() {
        if (status == ComplaintStatus.PENDING_INFO) {
            this.status = ComplaintStatus.IN_PROGRESS;
        }
    }

    public void escalate(UUID escalateTo) {
        if (status == ComplaintStatus.IN_PROGRESS || status == ComplaintStatus.ASSIGNED) {
            this.escalatedTo = escalateTo;
            this.escalatedAt = LocalDateTime.now();
            this.status = ComplaintStatus.ESCALATED;
        }
    }

    public void resolve(UUID resolvedBy, String notes) {
        if (status.canBeResolved()) {
            this.resolvedBy = resolvedBy;
            this.resolutionNotes = notes;
            this.resolvedAt = LocalDateTime.now();
            this.status = ComplaintStatus.RESOLVED;
        }
    }

    public void reject(UUID rejectedBy, String reason) {
        if (status == ComplaintStatus.ESCALATED) {
            this.resolvedBy = rejectedBy;
            this.resolutionNotes = reason;
            this.resolvedAt = LocalDateTime.now();
            this.status = ComplaintStatus.REJECTED;
        }
    }

    public void close() {
        if (status == ComplaintStatus.RESOLVED || status == ComplaintStatus.REJECTED) {
            this.closedAt = LocalDateTime.now();
            this.status = ComplaintStatus.CLOSED;
        }
    }

    public void reopen() {
        if (status == ComplaintStatus.CLOSED) {
            this.status = ComplaintStatus.REOPENED;
        }
    }

    // Constructors
    public Complaint() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = ComplaintStatus.SUBMITTED;
        this.priority = ComplaintPriority.MEDIUM;
    }

    public Complaint(UUID customerId, String subject, String description) {
        this();
        this.customerId = customerId;
        this.subject = subject;
        this.description = description;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ComplaintPriority getPriority() { return priority; }
    public void setPriority(ComplaintPriority priority) { this.priority = priority; }
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public UUID getAssignedAgentId() { return assignedAgentId; }
    public void setAssignedAgentId(UUID assignedAgentId) { this.assignedAgentId = assignedAgentId; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public UUID getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }
    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }
    public UUID getEscalatedTo() { return escalatedTo; }
    public void setEscalatedTo(UUID escalatedTo) { this.escalatedTo = escalatedTo; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

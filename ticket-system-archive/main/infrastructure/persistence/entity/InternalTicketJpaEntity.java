package com.ziyara.backend.infrastructure.persistence.entity;

import com.ziyara.backend.domain.enums.TicketPriority;
import com.ziyara.backend.domain.enums.TicketStatus;
import com.ziyara.backend.domain.enums.TicketType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for InternalTicket
 * Infrastructure layer - handles database mapping
 */
@Entity
@Table(name = "support_internal_tickets")
public class InternalTicketJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "ticket_number", unique = true, nullable = false, length = 20)
    private String ticketNumber;
    
    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TicketType type;
    
    @Column(name = "subject", nullable = false)
    private String subject;
    
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private TicketPriority priority;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status;
    
    @Column(name = "module", length = 100)
    private String module;
    
    @Column(name = "sub_module", length = 100)
    private String subModule;
    
    @Column(name = "environment", length = 50)
    private String environment;
    
    @Column(name = "browser", length = 100)
    private String browser;
    
    @Column(name = "operating_system", length = 100)
    private String operatingSystem;
    
    @Column(name = "steps_to_reproduce", columnDefinition = "TEXT")
    private String stepsToReproduce;
    
    @Column(name = "expected_behavior", columnDefinition = "TEXT")
    private String expectedBehavior;
    
    @Column(name = "actual_behavior", columnDefinition = "TEXT")
    private String actualBehavior;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private String attachments;
    
    @Column(name = "assigned_to_id")
    private UUID assignedToId;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @Column(name = "estimated_hours", precision = 5, scale = 2)
    private BigDecimal estimatedHours;
    
    @Column(name = "actual_hours", precision = 5, scale = 2)
    private BigDecimal actualHours;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by")
    private UUID resolvedBy;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "verified_by")
    private UUID verifiedBy;
    
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    
    @Column(name = "closed_by")
    private UUID closedBy;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "cancelled_by")
    private UUID cancelledBy;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Column(name = "related_ticket_id")
    private UUID relatedTicketId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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
    public void setEstimatedHours(BigDecimal estimatedHours) { this.estimatedHours = estimatedHours; }
    public BigDecimal getActualHours() { return actualHours; }
    public void setActualHours(BigDecimal actualHours) { this.actualHours = actualHours; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
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

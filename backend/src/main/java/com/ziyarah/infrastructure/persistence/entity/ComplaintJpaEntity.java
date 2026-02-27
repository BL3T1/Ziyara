package com.ziyarah.infrastructure.persistence.entity;

import com.ziyarah.domain.enums.ComplaintPriority;
import com.ziyarah.domain.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity: ComplaintJpaEntity
 * Infrastructure layer representation of Complaint
 * Maps to 'complaints' table in PostgreSQL
 */
@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "ticket_number", nullable = false, unique = true, length = 20)
    private String ticketNumber;
    
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    
    @Column(name = "booking_id")
    private UUID bookingId;
    
    @Column(name = "subject", nullable = false)
    private String subject;
    
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private ComplaintPriority priority;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ComplaintStatus status;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by")
    private UUID resolvedBy;
    
    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;
    
    @Column(name = "escalated_to")
    private UUID escalatedTo;
    
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ComplaintStatus.SUBMITTED;
        }
        if (priority == null) {
            priority = ComplaintPriority.MEDIUM;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

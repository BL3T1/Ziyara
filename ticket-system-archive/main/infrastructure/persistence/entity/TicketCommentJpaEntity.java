package com.ziyara.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for TicketComment
 * Infrastructure layer - handles database mapping
 */
@Entity
@Table(name = "support_ticket_comments")
public class TicketCommentJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "is_internal", nullable = false)
    private boolean isInternal = true;
    
    @Column(name = "is_resolution", nullable = false)
    private boolean isResolution = false;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private String attachments;
    
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
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public boolean isInternal() { return isInternal; }
    public void setInternal(boolean internal) { isInternal = internal; }
    public boolean isResolution() { return isResolution; }
    public void setResolution(boolean resolution) { isResolution = resolution; }
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

package com.ziyarah.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: TicketComment
 * Comments and updates on internal tickets
 * No framework dependencies - pure Java
 */
public class TicketComment {
    
    private UUID id;
    private UUID ticketId;
    private UUID userId;
    private String comment;
    private boolean isInternal;
    private boolean isResolution;
    private String attachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public TicketComment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isInternal = true;
        this.isResolution = false;
    }

    public TicketComment(UUID ticketId, UUID userId, String comment) {
        this();
        this.ticketId = ticketId;
        this.userId = userId;
        this.comment = comment;
    }

    // Domain behavior
    public void markAsResolution() {
        this.isResolution = true;
    }

    public void markAsPublic() {
        this.isInternal = false;
    }

    public void markAsInternal() {
        this.isInternal = true;
    }

    public void updateComment(String newComment) {
        this.comment = newComment;
        this.updatedAt = LocalDateTime.now();
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

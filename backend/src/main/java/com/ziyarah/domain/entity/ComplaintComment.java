package com.ziyarah.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: ComplaintComment
 * Represents individual entries in a complaint conversation
 */
public class ComplaintComment {
    
    private UUID id;
    private UUID complaintId;
    private UUID userId;
    private String comment;
    private boolean isInternal; // Visible only to staff
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public ComplaintComment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isInternal = false;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getComplaintId() { return complaintId; }
    public void setComplaintId(UUID complaintId) { this.complaintId = complaintId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public boolean isInternal() { return isInternal; }
    public void setInternal(boolean internal) { isInternal = internal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

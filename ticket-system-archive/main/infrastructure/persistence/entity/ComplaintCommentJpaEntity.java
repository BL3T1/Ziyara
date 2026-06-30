package com.ziyara.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity: ComplaintCommentJpaEntity
 * Maps to 'complaint_comments' table
 */
@Entity
@Table(name = "support_complaint_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintCommentJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "complaint_id", nullable = false)
    private UUID complaintId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "is_internal")
    private Boolean isInternal = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
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

    public UUID getId() { return id; }
    public UUID getComplaintId() { return complaintId; }
    public UUID getUserId() { return userId; }
    public String getComment() { return comment; }
    public Boolean getIsInternal() { return isInternal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static ComplaintCommentJpaEntityBuilder builder() {
        return new ComplaintCommentJpaEntityBuilder();
    }

    public static class ComplaintCommentJpaEntityBuilder {
        private UUID id;
        private UUID complaintId;
        private UUID userId;
        private String comment;
        private Boolean isInternal;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ComplaintCommentJpaEntityBuilder id(UUID id) { this.id = id; return this; }
        public ComplaintCommentJpaEntityBuilder complaintId(UUID complaintId) { this.complaintId = complaintId; return this; }
        public ComplaintCommentJpaEntityBuilder userId(UUID userId) { this.userId = userId; return this; }
        public ComplaintCommentJpaEntityBuilder comment(String comment) { this.comment = comment; return this; }
        public ComplaintCommentJpaEntityBuilder isInternal(Boolean isInternal) { this.isInternal = isInternal; return this; }
        public ComplaintCommentJpaEntityBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ComplaintCommentJpaEntityBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public ComplaintCommentJpaEntity build() {
            ComplaintCommentJpaEntity e = new ComplaintCommentJpaEntity();
            e.setId(id); e.setComplaintId(complaintId); e.setUserId(userId);
            e.setComment(comment); e.setIsInternal(isInternal != null ? isInternal : false);
            e.setCreatedAt(createdAt); e.setUpdatedAt(updatedAt);
            return e;
        }
    }
}

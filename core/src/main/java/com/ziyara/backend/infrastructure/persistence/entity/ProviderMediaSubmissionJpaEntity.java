package com.ziyara.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "provider_media_submissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderMediaSubmissionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "image_type", nullable = false, length = 64)
    private String imageType;

    @Column(name = "context_key", length = 128)
    private String contextKey;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "is_primary")
    private Boolean primary = false;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "PENDING";

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (submittedAt == null) submittedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

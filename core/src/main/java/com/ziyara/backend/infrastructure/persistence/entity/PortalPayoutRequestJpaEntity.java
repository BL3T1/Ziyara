package com.ziyara.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portal_payout_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalPayoutRequestJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "scheduled_at")
    private String scheduledAt;

    @Column(name = "is_manual")
    private boolean manual;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
    }
}

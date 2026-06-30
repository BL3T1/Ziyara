package com.ziyara.backend.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

public class PortalPayoutRequest {

    private UUID id;
    private UUID providerId;
    private BigDecimal amount;
    private String currency;
    private String notes;
    private String status;
    private Instant requestedAt;
    private Instant processedAt;
    private UUID processedBy;
    private String rejectionReason;
    private String transactionId;
    private String scheduledAt;
    private boolean manual;

    // ── State transitions ─────────────────────────────────────────────────────

    public void approve(UUID actorId, String notes) {
        requireStatus("PENDING", "SCHEDULED");
        this.status = "PROCESSING";
        this.processedAt = Instant.now();
        this.processedBy = actorId;
        if (notes != null) this.notes = notes;
    }

    public void hold() {
        requireStatus("PENDING");
        this.status = "ON_HOLD";
    }

    public void releaseHold() {
        requireStatus("ON_HOLD");
        this.status = "PENDING";
    }

    public void cancel() {
        requireStatus("PENDING", "ON_HOLD", "SCHEDULED");
        this.status = "CANCELLED";
    }

    public void retry() {
        requireStatus("FAILED", "REJECTED");
        this.status = "PENDING";
    }

    public void markPaid(UUID actorId, String transactionId, String notes) {
        this.status = "COMPLETED";
        this.processedAt = Instant.now();
        this.processedBy = actorId;
        if (transactionId != null) this.transactionId = transactionId;
        if (notes != null) this.notes = notes;
    }

    public void schedule(String scheduledAt) {
        requireStatus("PENDING");
        this.status = "SCHEDULED";
        this.scheduledAt = scheduledAt;
    }

    private void requireStatus(String... allowed) {
        for (String s : allowed) {
            if (s.equals(this.status)) return;
        }
        throw new IllegalStateException(
                "Cannot perform action on payout in status " + this.status +
                ". Allowed: " + Arrays.toString(allowed));
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public UUID getProcessedBy() { return processedBy; }
    public void setProcessedBy(UUID processedBy) { this.processedBy = processedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }
    public boolean isManual() { return manual; }
    public void setManual(boolean manual) { this.manual = manual; }
}

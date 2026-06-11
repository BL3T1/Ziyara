package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.CashCollectionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: CashCollection
 * Records cash received by a provider against a booking's payment.
 * Lifecycle: OPEN → RECONCILED (admin) or DISPUTED (admin).
 */
public class CashCollection {

    private UUID id;
    private UUID paymentId;
    private UUID providerId;
    private LocalDateTime collectedAt;
    private UUID collectedByUserId;
    private BigDecimal amount;
    private String currency;
    private String receiptNumber;
    private String notes;
    private LocalDateTime reconciledAt;
    private UUID reconciledByUserId;
    private CashCollectionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CashCollection() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = CashCollectionStatus.OPEN;
        this.currency = "USD";
    }

    public void reconcile(UUID adminUserId) {
        this.status = CashCollectionStatus.RECONCILED;
        this.reconciledByUserId = adminUserId;
        this.reconciledAt = LocalDateTime.now();
    }

    public void dispute(UUID adminUserId, String reason) {
        this.status = CashCollectionStatus.DISPUTED;
        this.reconciledByUserId = adminUserId;
        this.reconciledAt = LocalDateTime.now();
        if (reason != null && !reason.isBlank()) {
            this.notes = (this.notes == null ? "" : this.notes + " | ") + "DISPUTE: " + reason;
        }
    }

    public boolean isOpen() {
        return status == CashCollectionStatus.OPEN;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
    public UUID getCollectedByUserId() { return collectedByUserId; }
    public void setCollectedByUserId(UUID collectedByUserId) { this.collectedByUserId = collectedByUserId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getReconciledAt() { return reconciledAt; }
    public void setReconciledAt(LocalDateTime reconciledAt) { this.reconciledAt = reconciledAt; }
    public UUID getReconciledByUserId() { return reconciledByUserId; }
    public void setReconciledByUserId(UUID reconciledByUserId) { this.reconciledByUserId = reconciledByUserId; }
    public CashCollectionStatus getStatus() { return status; }
    public void setStatus(CashCollectionStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.PaymentMethod;
import com.ziyara.backend.domain.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: Payment
 * Represents a financial transaction
 */
public class Payment {
    
    private UUID id;
    private UUID bookingId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private PaymentStatus status;
    private String transactionReference;
    /** External gateway transaction ID (3DS callback, webhook idempotency). */
    private String gatewayReference;
    /** 3DS status: AUTHENTICATED, NOT_REQUIRED, FAILED. */
    private String threeDsStatus;
    /** Full gateway response for debugging (optional). */
    private String gatewayResponse;
    private String gatewayName;
    private String paymentToken;
    /** Idempotency key to prevent duplicate charges (client-supplied). */
    private String idempotencyKey;
    private String errorMessage;
    private String entityType;
    private UUID entityId;
    private String category;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Domain behavior methods
    public boolean isSuccessful() {
        return status == PaymentStatus.COMPLETED;
    }

    public void complete(String reference) {
        this.status = PaymentStatus.COMPLETED;
        this.transactionReference = reference;
        this.processedAt = LocalDateTime.now();
    }

    public void fail(String error) {
        this.status = PaymentStatus.FAILED;
        this.errorMessage = error;
        this.processedAt = LocalDateTime.now();
    }

    // Constructors
    public Payment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
        this.currency = "USD";
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public String getGatewayReference() { return gatewayReference; }
    public void setGatewayReference(String gatewayReference) { this.gatewayReference = gatewayReference; }
    public String getThreeDsStatus() { return threeDsStatus; }
    public void setThreeDsStatus(String threeDsStatus) { this.threeDsStatus = threeDsStatus; }
    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    public String getGatewayName() { return gatewayName; }
    public void setGatewayName(String gatewayName) { this.gatewayName = gatewayName; }
    public String getPaymentToken() { return paymentToken; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

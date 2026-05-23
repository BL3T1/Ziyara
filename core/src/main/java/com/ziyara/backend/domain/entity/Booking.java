package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain Entity: Booking
 * Core booking entity representing reservations
 * No framework dependencies - pure Java
 */
public class Booking {

    private UUID id;
    private String bookingReference;
    private UUID customerId;
    private UUID serviceId;
    private UUID discountCodeId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int guests;
    private int rooms;
    private BigDecimal baseAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal commissionAmount;
    private BigDecimal totalAmount;
    private String currency;
    private BookingStatus status;
    private String specialRequests;
    private String idDocumentUrl;
    private boolean idDocumentVerified;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private UUID cancelledBy;
    private String rejectionReason;
    private String delayReason;
    private String internalNotes;
    private LocalDateTime rejectedAt;
    private UUID rejectedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Snapshot for discount scope when applying codes (restaurant / room-type). */
    private List<UUID> discountContextMenuItemIds;
    private List<UUID> discountContextMenuSectionIds;
    private UUID discountContextRoomTypeId;

    // Domain behavior methods
    public boolean canBeCancelled() {
        return status.canBeCancelled();
    }

    public boolean canBeModified() {
        return status.canBeModified();
    }

    public boolean isActive() {
        return status == BookingStatus.ACTIVE;
    }

    public boolean isPending() {
        return status == BookingStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public boolean isCompleted() {
        return status == BookingStatus.COMPLETED;
    }

    public void confirm() {
        if (status == BookingStatus.PENDING) {
            this.status = BookingStatus.CONFIRMED;
            this.confirmedAt = LocalDateTime.now();
        }
    }

    public void activate() {
        if (status == BookingStatus.CONFIRMED) {
            this.status = BookingStatus.ACTIVE;
        }
    }

    public void complete() {
        if (status == BookingStatus.ACTIVE) {
            this.status = BookingStatus.COMPLETED;
        }
    }

    public void cancel(UUID cancelledBy, String reason) {
        if (canBeCancelled()) {
            this.status = BookingStatus.CANCELLED;
            this.cancelledAt = LocalDateTime.now();
            this.cancelledBy = cancelledBy;
            this.cancellationReason = reason;
        }
    }

    public void expire() {
        if (status == BookingStatus.PENDING) {
            this.status = BookingStatus.EXPIRED;
        }
    }

    public void reject(UUID rejectedBy, String reason) {
        this.status = BookingStatus.CANCELLED;
        this.rejectionReason = reason;
        this.rejectedBy = rejectedBy;
        this.rejectedAt = LocalDateTime.now();
    }

    public BigDecimal calculateRefundAmount() {
        if (!canBeCancelled()) {
            return BigDecimal.ZERO;
        }

        LocalDate now = LocalDate.now();

        // Before check-in: 100% refund
        if (now.isBefore(checkInDate)) {
            return totalAmount;
        }

        // During service: 95% refund (5% penalty)
        if (checkOutDate != null && now.isBefore(checkOutDate)) {
            return totalAmount.multiply(new BigDecimal("0.95"));
        }

        // After service: no refund
        return BigDecimal.ZERO;
    }

    public BigDecimal calculatePenaltyAmount() {
        BigDecimal refundAmount = calculateRefundAmount();
        return totalAmount.subtract(refundAmount);
    }

    public void applyDiscount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        this.totalAmount = baseAmount.subtract(discountAmount).add(taxAmount);
    }

    public void calculateCommission(BigDecimal commissionRate) {
        this.commissionAmount = totalAmount.multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
    }

    // Constructors
    public Booking() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = BookingStatus.PENDING;
        this.currency = "USD";
        this.guests = 1;
        this.rooms = 1;
        this.discountAmount = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.commissionAmount = BigDecimal.ZERO;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getDiscountCodeId() {
        return discountCodeId;
    }

    public void setDiscountCodeId(UUID discountCodeId) {
        this.discountCodeId = discountCodeId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public int getGuests() {
        return guests;
    }

    public void setGuests(int guests) {
        this.guests = guests;
    }

    public int getRooms() {
        return rooms;
    }

    public void setRooms(int rooms) {
        this.rooms = rooms;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public String getIdDocumentUrl() {
        return idDocumentUrl;
    }

    public void setIdDocumentUrl(String idDocumentUrl) {
        this.idDocumentUrl = idDocumentUrl;
    }

    public boolean isIdDocumentVerified() {
        return idDocumentVerified;
    }

    public void setIdDocumentVerified(boolean idDocumentVerified) {
        this.idDocumentVerified = idDocumentVerified;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public UUID getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(UUID cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getDelayReason() { return delayReason; }
    public void setDelayReason(String delayReason) { this.delayReason = delayReason; }

    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public UUID getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(UUID rejectedBy) { this.rejectedBy = rejectedBy; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<UUID> getDiscountContextMenuItemIds() {
        return discountContextMenuItemIds;
    }

    public void setDiscountContextMenuItemIds(List<UUID> discountContextMenuItemIds) {
        this.discountContextMenuItemIds = discountContextMenuItemIds;
    }

    public List<UUID> getDiscountContextMenuSectionIds() {
        return discountContextMenuSectionIds;
    }

    public void setDiscountContextMenuSectionIds(List<UUID> discountContextMenuSectionIds) {
        this.discountContextMenuSectionIds = discountContextMenuSectionIds;
    }

    public UUID getDiscountContextRoomTypeId() {
        return discountContextRoomTypeId;
    }

    public void setDiscountContextRoomTypeId(UUID discountContextRoomTypeId) {
        this.discountContextRoomTypeId = discountContextRoomTypeId;
    }
}

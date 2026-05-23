package com.ziyara.backend.application.dto;

import com.ziyara.backend.domain.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO: BookingResponse
 * Response body for booking data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Booking response")
public class BookingResponse {
    
    @Schema(description = "Booking ID")
    private UUID id;
    
    @Schema(description = "Booking reference")
    private String bookingReference;
    
    @Schema(description = "Customer ID")
    private UUID customerId;
    
    @Schema(description = "Service ID")
    private UUID serviceId;
    
    @Schema(description = "Service name")
    private String serviceName;
    
    @Schema(description = "Service type")
    private String serviceType;
    
    @Schema(description = "Check-in date")
    private LocalDate checkInDate;
    
    @Schema(description = "Check-out date")
    private LocalDate checkOutDate;
    
    @Schema(description = "Number of guests")
    private Integer guests;
    
    @Schema(description = "Number of rooms")
    private Integer rooms;
    
    @Schema(description = "Base amount")
    private BigDecimal baseAmount;
    
    @Schema(description = "Discount amount")
    private BigDecimal discountAmount;
    
    @Schema(description = "Tax amount")
    private BigDecimal taxAmount;
    
    @Schema(description = "Commission amount")
    private BigDecimal commissionAmount;
    
    @Schema(description = "Total amount")
    private BigDecimal totalAmount;
    
    @Schema(description = "Currency")
    private String currency;
    
    @Schema(description = "Booking status")
    private BookingStatus status;
    
    @Schema(description = "Special requests")
    private String specialRequests;
    
    @Schema(description = "ID document verified")
    private Boolean idDocumentVerified;
    
    @Schema(description = "Confirmed at")
    private LocalDateTime confirmedAt;
    
    @Schema(description = "Cancelled at")
    private LocalDateTime cancelledAt;
    
    @Schema(description = "Cancellation reason")
    private String cancellationReason;

    @Schema(description = "Rejection reason (provider-supplied)")
    private String rejectionReason;

    @Schema(description = "Delay reason")
    private String delayReason;

    @Schema(description = "Internal staff notes")
    private String internalNotes;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;
    
    @Schema(description = "Can be cancelled")
    private Boolean canBeCancelled;
    
    @Schema(description = "Can be modified")
    private Boolean canBeModified;

    public static BookingResponseBuilder builder() {
        return new BookingResponseBuilder();
    }

    public static class BookingResponseBuilder {
        private UUID id;
        private String bookingReference;
        private UUID customerId;
        private UUID serviceId;
        private String serviceName;
        private String serviceType;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private Integer guests;
        private Integer rooms;
        private BigDecimal baseAmount;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal commissionAmount;
        private BigDecimal totalAmount;
        private String currency;
        private BookingStatus status;
        private String specialRequests;
        private Boolean idDocumentVerified;
        private LocalDateTime confirmedAt;
        private LocalDateTime cancelledAt;
        private String cancellationReason;
        private String rejectionReason;
        private String delayReason;
        private String internalNotes;
        private LocalDateTime createdAt;
        private Boolean canBeCancelled;
        private Boolean canBeModified;

        public BookingResponseBuilder id(UUID id) { this.id = id; return this; }
        public BookingResponseBuilder bookingReference(String v) { this.bookingReference = v; return this; }
        public BookingResponseBuilder customerId(UUID v) { this.customerId = v; return this; }
        public BookingResponseBuilder serviceId(UUID v) { this.serviceId = v; return this; }
        public BookingResponseBuilder serviceName(String v) { this.serviceName = v; return this; }
        public BookingResponseBuilder serviceType(String v) { this.serviceType = v; return this; }
        public BookingResponseBuilder checkInDate(LocalDate v) { this.checkInDate = v; return this; }
        public BookingResponseBuilder checkOutDate(LocalDate v) { this.checkOutDate = v; return this; }
        public BookingResponseBuilder guests(Integer v) { this.guests = v; return this; }
        public BookingResponseBuilder rooms(Integer v) { this.rooms = v; return this; }
        public BookingResponseBuilder baseAmount(BigDecimal v) { this.baseAmount = v; return this; }
        public BookingResponseBuilder discountAmount(BigDecimal v) { this.discountAmount = v; return this; }
        public BookingResponseBuilder taxAmount(BigDecimal v) { this.taxAmount = v; return this; }
        public BookingResponseBuilder commissionAmount(BigDecimal v) { this.commissionAmount = v; return this; }
        public BookingResponseBuilder totalAmount(BigDecimal v) { this.totalAmount = v; return this; }
        public BookingResponseBuilder currency(String v) { this.currency = v; return this; }
        public BookingResponseBuilder status(BookingStatus v) { this.status = v; return this; }
        public BookingResponseBuilder specialRequests(String v) { this.specialRequests = v; return this; }
        public BookingResponseBuilder idDocumentVerified(Boolean v) { this.idDocumentVerified = v; return this; }
        public BookingResponseBuilder confirmedAt(LocalDateTime v) { this.confirmedAt = v; return this; }
        public BookingResponseBuilder cancelledAt(LocalDateTime v) { this.cancelledAt = v; return this; }
        public BookingResponseBuilder cancellationReason(String v) { this.cancellationReason = v; return this; }
        public BookingResponseBuilder rejectionReason(String v) { this.rejectionReason = v; return this; }
        public BookingResponseBuilder delayReason(String v) { this.delayReason = v; return this; }
        public BookingResponseBuilder internalNotes(String v) { this.internalNotes = v; return this; }
        public BookingResponseBuilder createdAt(LocalDateTime v) { this.createdAt = v; return this; }
        public BookingResponseBuilder canBeCancelled(Boolean v) { this.canBeCancelled = v; return this; }
        public BookingResponseBuilder canBeModified(Boolean v) { this.canBeModified = v; return this; }
        public BookingResponse build() {
            BookingResponse r = new BookingResponse();
            r.setId(id); r.setBookingReference(bookingReference); r.setCustomerId(customerId);
            r.setServiceId(serviceId); r.setServiceName(serviceName); r.setServiceType(serviceType);
            r.setCheckInDate(checkInDate); r.setCheckOutDate(checkOutDate); r.setGuests(guests); r.setRooms(rooms);
            r.setBaseAmount(baseAmount); r.setDiscountAmount(discountAmount); r.setTaxAmount(taxAmount);
            r.setCommissionAmount(commissionAmount); r.setTotalAmount(totalAmount); r.setCurrency(currency);
            r.setStatus(status); r.setSpecialRequests(specialRequests); r.setIdDocumentVerified(idDocumentVerified);
            r.setConfirmedAt(confirmedAt); r.setCancelledAt(cancelledAt); r.setCancellationReason(cancellationReason);
            r.setRejectionReason(rejectionReason); r.setDelayReason(delayReason); r.setInternalNotes(internalNotes);
            r.setCreatedAt(createdAt); r.setCanBeCancelled(canBeCancelled); r.setCanBeModified(canBeModified);
            return r;
        }
    }
}

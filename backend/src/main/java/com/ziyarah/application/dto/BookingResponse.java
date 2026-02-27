package com.ziyarah.application.dto;

import com.ziyarah.domain.enums.BookingStatus;
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
    
    @Schema(description = "Created at")
    private LocalDateTime createdAt;
    
    @Schema(description = "Can be cancelled")
    private Boolean canBeCancelled;
    
    @Schema(description = "Can be modified")
    private Boolean canBeModified;
}

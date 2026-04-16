package com.ziyara.backend.application.dto;

import com.ziyara.backend.domain.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO: BookingRequest
 * Request body for creating a booking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Booking creation request")
public class BookingRequest {
    
    @NotNull(message = "Service ID is required")
    @Schema(description = "Service ID to book", required = true)
    private UUID serviceId;
    
    @Schema(description = "Discount code to apply")
    private String discountCode;
    
    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    @Schema(description = "Check-in date", required = true, example = "2026-03-01")
    private LocalDate checkInDate;
    
    @Future(message = "Check-out date must be in the future")
    @Schema(description = "Check-out date", example = "2026-03-05")
    private LocalDate checkOutDate;
    
    @Min(value = 1, message = "At least 1 guest is required")
    @Max(value = 50, message = "Maximum 50 guests allowed")
    @Schema(description = "Number of guests", example = "2")
    private Integer guests = 1;
    
    @Min(value = 1, message = "At least 1 room is required")
    @Max(value = 20, message = "Maximum 20 rooms allowed")
    @Schema(description = "Number of rooms", example = "1")
    private Integer rooms = 1;
    
    @Schema(description = "Special requests")
    @Size(max = 1000, message = "Special requests must not exceed 1000 characters")
    private String specialRequests;
    
    @Schema(description = "ID document URL (uploaded separately)")
    private String idDocumentUrl;

    @Schema(description = "Menu item IDs for scoped restaurant discounts")
    private List<UUID> menuItemIds;

    @Schema(description = "Menu section IDs for scoped restaurant discounts")
    private List<UUID> menuSectionIds;

    @Schema(description = "Room type ID for scoped hotel/resort discounts")
    private UUID roomTypeId;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    @Schema(description = "Currency code", example = "USD")
    private String currency = "USD";

    public UUID getServiceId() { return serviceId; }
    public String getDiscountCode() { return discountCode; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public Integer getGuests() { return guests != null ? guests : 1; }
    public Integer getRooms() { return rooms != null ? rooms : 1; }
    public String getSpecialRequests() { return specialRequests; }
    public String getIdDocumentUrl() { return idDocumentUrl; }
    public String getCurrency() { return currency; }
}

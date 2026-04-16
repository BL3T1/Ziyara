package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request to apply a discount code to a booking (POST /discounts/apply).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Apply discount to a booking")
public class ApplyDiscountToBookingRequest {

    @NotBlank(message = "Discount code is required")
    @Schema(description = "The code to apply", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotNull(message = "Booking ID is required")
    @Schema(description = "Booking to apply the discount to", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID bookingId;
}

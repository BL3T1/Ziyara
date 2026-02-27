package com.ziyarah.application.dto.request;

import com.ziyarah.domain.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a payment")
public class CreatePaymentRequest {
    
    @NotNull(message = "Booking ID is required")
    @Schema(description = "Associated booking ID")
    private UUID bookingId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Schema(description = "Amount to pay")
    private BigDecimal amount;
    
    @Schema(description = "Currency code (3 chars)")
    private String currency;
    
    @NotNull(message = "Payment method is required")
    @Schema(description = "Selected payment method")
    private PaymentMethod method;
    
    @Schema(description = "Optional provider-specific payment token")
    private String paymentToken;
}

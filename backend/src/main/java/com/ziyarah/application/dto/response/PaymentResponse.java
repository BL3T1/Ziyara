package com.ziyarah.application.dto.response;

import com.ziyarah.domain.enums.PaymentMethod;
import com.ziyarah.domain.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment details")
public class PaymentResponse {
    
    @Schema(description = "Payment ID")
    private UUID id;
    
    @Schema(description = "Booking ID")
    private UUID bookingId;
    
    @Schema(description = "Payment amount")
    private BigDecimal amount;
    
    @Schema(description = "Currency")
    private String currency;
    
    @Schema(description = "Payment method")
    private PaymentMethod method;
    
    @Schema(description = "Payment status")
    private PaymentStatus status;
    
    @Schema(description = "Transaction reference")
    private String transactionReference;
    
    @Schema(description = "Payment gateway name")
    private String gatewayName;
    
    @Schema(description = "Error message (if failed)")
    private String errorMessage;
    
    @Schema(description = "Processed timestamp")
    private LocalDateTime processedAt;
    
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}

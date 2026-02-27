package com.ziyarah.application.dto.response;

import com.ziyarah.domain.enums.RefundStatus;
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
@Schema(description = "Refund details")
public class RefundResponse {
    
    @Schema(description = "Refund ID")
    private UUID id;
    
    @Schema(description = "Associated payment ID")
    private UUID paymentId;
    
    @Schema(description = "Refund amount")
    private BigDecimal amount;
    
    @Schema(description = "Currency")
    private String currency;
    
    @Schema(description = "Refund status")
    private RefundStatus status;
    
    @Schema(description = "Refund reason")
    private String reason;
    
    @Schema(description = "Transaction reference")
    private String transactionReference;
    
    @Schema(description = "Processed timestamp")
    private LocalDateTime processedAt;
    
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}

package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Refund response")
public class RefundResponse {

    private UUID id;
    private UUID paymentId;
    private BigDecimal amount;
    private String currency;
    private RefundStatus status;
    private String reason;
    private UUID processedBy;
    private String transactionReference;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

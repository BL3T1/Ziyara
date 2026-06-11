package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.CashCollectionStatus;
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
@Schema(description = "Cash collection ledger entry")
public class CashCollectionResponse {

    private UUID id;
    private UUID paymentId;
    private UUID providerId;
    private LocalDateTime collectedAt;
    private UUID collectedByUserId;
    private BigDecimal amount;
    private String currency;
    private String receiptNumber;
    private String notes;
    private LocalDateTime reconciledAt;
    private UUID reconciledByUserId;
    private CashCollectionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

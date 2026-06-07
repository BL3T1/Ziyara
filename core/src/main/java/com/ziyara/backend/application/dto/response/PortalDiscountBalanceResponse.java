package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Provider self-discount balance")
public class PortalDiscountBalanceResponse {
    private UUID providerId;
    private String currency;
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;
    private BigDecimal availableAmount;
}

package com.ziyarah.application.dto.response;

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
@Schema(description = "Exchange rate details")
public class ExchangeRateResponse {
    
    @Schema(description = "Internal ID")
    private UUID id;
    
    @Schema(description = "Source currency")
    private String fromCurrency;
    
    @Schema(description = "Target currency")
    private String toCurrency;
    
    @Schema(description = "Conversion rate")
    private BigDecimal rate;
    
    @Schema(description = "Provider name")
    private String provider;
    
    @Schema(description = "Effective timestamp")
    private LocalDateTime effectiveAt;
}

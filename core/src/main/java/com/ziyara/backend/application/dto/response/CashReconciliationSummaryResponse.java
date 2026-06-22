package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Aggregated cash reconciliation totals per provider over a period.")
public class CashReconciliationSummaryResponse {

    private LocalDate start;
    private LocalDate end;
    private BigDecimal totalOpen;
    private BigDecimal totalReconciled;
    private BigDecimal totalDisputed;
    private List<ProviderTotal> providers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProviderTotal {
        private UUID providerId;
        private String providerName;
        private BigDecimal openAmount;
        private BigDecimal reconciledAmount;
        private int openCount;
        private int reconciledCount;
    }
}

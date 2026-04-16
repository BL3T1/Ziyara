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
@Schema(description = "Dashboard: provider payouts in period")
public class PayoutSummaryResponse {

    @Schema(description = "Start date of period")
    private LocalDate start;

    @Schema(description = "End date of period")
    private LocalDate end;

    @Schema(description = "Payouts per provider")
    private List<PayoutItem> payouts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PayoutItem {
        private UUID providerId;
        private String providerName;
        private BigDecimal amount;
        private String currency;
    }
}

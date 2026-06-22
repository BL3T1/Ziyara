package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Provider records cash received from a customer.")
public class RecordCashCollectionRequest {

    @NotNull
    @Positive
    @Schema(description = "Amount received in cash", example = "120.00")
    private BigDecimal amount;

    @Schema(description = "Currency code (defaults to payment's currency)", example = "USD")
    private String currency;

    @Schema(description = "Timestamp the cash was received (defaults to now)")
    private LocalDateTime collectedAt;

    @Schema(description = "Optional free-text notes (e.g. denominations, customer name)")
    private String notes;
}

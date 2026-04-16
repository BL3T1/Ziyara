package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update an exchange rate")
public class UpdateExchangeRateRequest {

    @DecimalMin("0.000001")
    @Schema(description = "Conversion rate")
    private BigDecimal rate;

    @Schema(description = "Effective date")
    private LocalDate effectiveDate;
}

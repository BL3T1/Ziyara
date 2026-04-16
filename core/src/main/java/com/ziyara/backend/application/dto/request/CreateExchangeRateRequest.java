package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create an exchange rate")
public class CreateExchangeRateRequest {

    @NotBlank
    @Schema(description = "From currency code", required = true)
    private String fromCurrency;

    @NotBlank
    @Schema(description = "To currency code", required = true)
    private String toCurrency;

    @NotNull
    @DecimalMin("0.000001")
    @Schema(description = "Conversion rate", required = true)
    private BigDecimal rate;

    @Schema(description = "Effective date (default today)")
    private LocalDate effectiveDate;
}

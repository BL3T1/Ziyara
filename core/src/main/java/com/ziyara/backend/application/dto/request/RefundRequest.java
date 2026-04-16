package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a refund for a payment")
public class RefundRequest {

    @NotNull
    @DecimalMin("0.01")
    @Schema(description = "Refund amount", required = true)
    private BigDecimal amount;

    @NotBlank(message = "Refund reason is required")
    @Size(max = 2000)
    @Schema(description = "Reason for refund (mandatory for audit)", required = true)
    private String reason;
}

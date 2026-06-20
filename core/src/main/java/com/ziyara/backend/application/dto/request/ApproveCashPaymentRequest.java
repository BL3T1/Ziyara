package com.ziyara.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ApproveCashPaymentRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    @Size(min = 3, max = 3)
    private String currency;

    @Size(max = 500)
    private String notes;
}

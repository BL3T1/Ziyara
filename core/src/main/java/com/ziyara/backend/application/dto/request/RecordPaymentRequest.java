package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RecordPaymentRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    private PaymentMethod method;

    @Size(max = 255)
    private String transactionReference;

    @Size(max = 500)
    private String notes;
}

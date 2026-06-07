package com.ziyara.backend.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateManualPayoutRequest {

    @NotNull
    private UUID providerId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String memo;

    /** If true, immediately sets status to PROCESSING; otherwise PENDING */
    private boolean executeImmediately;
}

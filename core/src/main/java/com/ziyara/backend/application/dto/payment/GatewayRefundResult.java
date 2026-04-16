package com.ziyara.backend.application.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a refund request to the gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayRefundResult {

    private boolean success;
    private String gatewayRefundId;
    private String errorMessage;
}

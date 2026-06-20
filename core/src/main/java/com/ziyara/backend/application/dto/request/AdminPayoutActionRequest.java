package com.ziyara.backend.application.dto.request;

import lombok.Data;

@Data
public class AdminPayoutActionRequest {
    private String notes;
    private String reason;
    private String transactionId;
    private String scheduledAt;
}

package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin view of a provider payout request")
public class AdminPayoutResponse {

    private UUID id;
    private UUID providerId;
    private String providerName;
    private String providerEmail;

    private BigDecimal amount;
    private String currency;
    private String notes;

    @Schema(description = "PENDING | ON_HOLD | SCHEDULED | PROCESSING | COMPLETED | FAILED | CANCELLED | REJECTED")
    private String status;

    private Instant requestedAt;
    private Instant processedAt;
    private String processedBy;
    private String rejectionReason;
    private String transactionId;
    private Instant scheduledAt;
    private boolean manual;

    @Schema(description = "Status history entries, newest first")
    private List<StatusHistoryEntry> statusHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryEntry {
        private String fromStatus;
        private String toStatus;
        private Instant timestamp;
        private String userId;
        private String userDisplay;
        private String notes;
    }
}

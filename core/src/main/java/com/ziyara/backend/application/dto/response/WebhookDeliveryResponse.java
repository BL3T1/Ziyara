package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Outbound webhook delivery record")
public class WebhookDeliveryResponse {
    private UUID id;
    private UUID subscriptionId;
    private String event;
    private String status;
    private Integer httpStatus;
    private int attemptCount;
    private Instant lastAttemptAt;
    private Instant createdAt;
}

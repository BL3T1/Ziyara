package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Outbound webhook subscription")
public class WebhookSubscriptionResponse {
    private UUID id;
    private UUID providerId;
    private String name;
    private String url;
    private List<String> events;
    private boolean active;
    private Instant createdAt;
    /** Returned only on creation — store it securely, it is not shown again. */
    private String secret;
}

package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Register a new outbound webhook subscription")
public class CreateWebhookSubscriptionRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Human-readable name for this subscription")
    private String name;

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "HTTPS endpoint that will receive POST requests")
    private String url;

    @NotEmpty
    @Schema(description = "Event types to subscribe to: booking.created, content.approved, payout.processed")
    private List<String> events;

    @Schema(description = "Scope to a specific provider (null = company-wide)")
    private UUID providerId;
}

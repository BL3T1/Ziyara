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
@Schema(description = "A support request submitted from the provider portal")
public class PortalSupportRequestResponse {

    @Schema(description = "Row id")
    private UUID id;

    @Schema(description = "Provider that submitted this request")
    private UUID providerId;

    @Schema(description = "Display name of the provider")
    private String providerName;

    @Schema(description = "Subject")
    private String subject;

    @Schema(description = "Message body")
    private String body;

    @Schema(description = "Submitting user id (if still present)")
    private UUID userId;

    @Schema(description = "Created timestamp (UTC)")
    private Instant createdAt;

    @Schema(description = "Staff response text, null if not yet responded")
    private String staffResponse;

    @Schema(description = "When staff responded (UTC), null if not yet responded")
    private Instant respondedAt;

    @Schema(description = "User id of the staff member who responded")
    private UUID respondedByUserId;
}

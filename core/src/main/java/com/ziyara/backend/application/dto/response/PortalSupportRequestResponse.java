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

    @Schema(description = "Subject")
    private String subject;

    @Schema(description = "Message body")
    private String body;

    @Schema(description = "Submitting user id (if still present)")
    private UUID userId;

    @Schema(description = "Created timestamp (UTC)")
    private Instant createdAt;
}

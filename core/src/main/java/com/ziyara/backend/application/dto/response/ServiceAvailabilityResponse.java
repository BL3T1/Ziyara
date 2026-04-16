package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for GET /services/{id}/availability.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Service availability check result")
public class ServiceAvailabilityResponse {
    @Schema(description = "Whether the service is available for the given date range")
    private boolean available;
    @Schema(description = "Optional message (e.g. reason when not available)")
    private String message;
}

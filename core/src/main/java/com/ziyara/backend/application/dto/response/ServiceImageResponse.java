package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.ServiceImageCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response for service image (GET /services/{id}/images).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Service image")
public class ServiceImageResponse {
    @Schema(description = "Image ID")
    private UUID id;
    @Schema(description = "Service ID")
    private UUID serviceId;
    @Schema(description = "Image URL")
    private String url;
    @Schema(description = "Alt text")
    private String altText;
    @Schema(description = "Whether this is the primary image")
    private boolean primary;
    @Schema(description = "Display order")
    private int displayOrder;
    @Schema(description = "Image category (property, room, trip, other)")
    private ServiceImageCategory category;
    @Schema(description = "Optional grouping key (e.g. room type label)")
    private String contextKey;
}

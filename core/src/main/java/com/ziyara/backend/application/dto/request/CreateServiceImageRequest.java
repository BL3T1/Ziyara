package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.ServiceImageCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create a service image via URL (MVP A — no multipart upload).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateServiceImageRequest {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "Image URL (CDN or absolute path)")
    private String url;

    @Size(max = 500)
    private String altText;

    @Schema(description = "Defaults to PROPERTY when omitted")
    private ServiceImageCategory category;

    @Size(max = 100)
    private String contextKey;

    private Boolean primary;

    private Integer displayOrder;
}

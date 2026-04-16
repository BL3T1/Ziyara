package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.ServiceImageCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Partial update; null fields are left unchanged.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateServiceImageRequest {

    @Size(max = 500)
    private String url;

    @Size(max = 500)
    private String altText;

    private ServiceImageCategory category;

    @Size(max = 100)
    private String contextKey;

    private Boolean primary;

    private Integer displayOrder;
}

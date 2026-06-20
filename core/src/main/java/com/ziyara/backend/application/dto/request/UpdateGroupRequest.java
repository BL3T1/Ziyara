package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Partial update for organizational group ({@code sys_groups}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update group display fields and optionally code (custom groups only; platform Z codes fixed)")
public class UpdateGroupRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String nameAr;

    @Size(max = 20)
    private String code;

    @Schema(description = "Description (English)")
    private String description;

    @Schema(description = "Description (Arabic)")
    private String descriptionAr;
}

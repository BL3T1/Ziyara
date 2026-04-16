package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update role display fields only (not code or permissions). System and custom roles supported.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Partial update for role name/description")
public class UpdateRoleRequest {

    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 200)
    private String nameAr;

    @Size(max = 1000)
    private String descriptionAr;
}

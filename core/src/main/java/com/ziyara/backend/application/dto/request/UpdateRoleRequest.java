package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@Schema(description = "Partial update for role name/description and optional group assignment")
public class UpdateRoleRequest {

    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 200)
    private String nameAr;

    @Size(max = 1000)
    private String descriptionAr;

    @Schema(description = "Assign role to this group (UUID). Ignored if removeFromGroup is true.")
    private java.util.UUID groupId;

    @Schema(description = "When true, clears the group assignment (sets groupId to null).")
    private Boolean removeFromGroup;

    @Min(0) @Max(100)
    @Schema(description = "Maximum discount percentage this role may approve (0–100); null = no change")
    private Short maxDiscountPct;

    @Schema(description = "Assignable to provider portal staff; null = no change")
    private Boolean providerRole;
}

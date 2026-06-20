package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Create organizational group (sys_groups)")
public class CreateGroupRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Display name (English)", example = "Regional Operations")
    private String name;

    @Size(max = 100)
    @Schema(description = "Display name (Arabic)")
    private String nameAr;

    @Size(max = 20)
    @Schema(description = "Unique code (letters, digits, underscore); leave empty to auto-assign next C{n}. Z+digits reserved for platform.", example = "C8")
    private String code;

    @Schema(description = "Description (English)")
    private String description;

    @Schema(description = "Description (Arabic)")
    private String descriptionAr;
}

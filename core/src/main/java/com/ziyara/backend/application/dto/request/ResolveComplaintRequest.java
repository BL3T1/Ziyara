package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to resolve a complaint")
public class ResolveComplaintRequest {

    @Schema(description = "Resolution notes")
    private String notes;
}

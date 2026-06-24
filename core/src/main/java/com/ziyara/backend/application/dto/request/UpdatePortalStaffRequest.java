package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Update portal staff metadata")
public class UpdatePortalStaffRequest {

    @Schema(description = "Job title label")
    private String title;

    @Schema(description = "Email address")
    private String email;
}

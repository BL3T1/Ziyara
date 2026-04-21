package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Organizational group (platform Z1–Z7 or custom)")
public class GroupResponse {
    @Schema(description = "Group ID") private UUID id;
    @Schema(description = "Group name") private String name;
    @Schema(description = "Group code") private String code;
    @Schema(description = "Description") private String description;
}

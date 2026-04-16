package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "Organizational group with role and staff counts")
public class GroupSummaryResponse {
    @Schema(description = "Group ID") private UUID id;
    @Schema(description = "Group name") private String name;
    @Schema(description = "Group code") private String code;
    @Schema(description = "Description") private String description;
    @Schema(description = "Number of roles in this group") private int roleCount;
    @Schema(description = "Total users assigned to roles in this group") private long userCount;
}

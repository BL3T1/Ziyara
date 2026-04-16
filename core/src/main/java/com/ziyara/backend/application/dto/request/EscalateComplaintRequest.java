package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to escalate a complaint")
public class EscalateComplaintRequest {

    @NotNull
    @Schema(description = "User ID to escalate to", required = true)
    private UUID escalateToId;
}

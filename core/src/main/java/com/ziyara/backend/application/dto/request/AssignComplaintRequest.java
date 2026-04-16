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
@Schema(description = "Request to assign a complaint to an agent")
public class AssignComplaintRequest {

    @NotNull
    @Schema(description = "Agent (user) ID to assign to", required = true)
    private UUID agentId;
}

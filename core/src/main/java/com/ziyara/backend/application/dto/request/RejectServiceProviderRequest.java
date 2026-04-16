package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reject a pending provider (optional reason for audit)")
public class RejectServiceProviderRequest {

    @Schema(description = "Optional reason shown in audit trail")
    private String reason;
}

package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Admin marks a cash collection as reconciled.")
public class ReconcileCashCollectionRequest {

    @Schema(description = "Optional reconciliation notes")
    private String notes;
}

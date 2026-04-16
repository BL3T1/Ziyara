package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Replace custom role sidebar visibility (ordered item ids)")
public class UpdateRoleNavigationRequest {
    @NotNull
    @Schema(description = "Known sidebar item ids only; empty list clears custom nav (fallback to user enum role)")
    private List<String> visibleItemIds;
}

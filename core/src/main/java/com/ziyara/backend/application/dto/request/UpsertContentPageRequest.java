package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Create or update website content page")
public class UpsertContentPageRequest {

    @NotNull
    @Schema(description = "English content payload (JSON object)", required = true)
    private Map<String, Object> contentEn;

    @NotNull
    @Schema(description = "Arabic content payload (JSON object)", required = true)
    private Map<String, Object> contentAr;

    @Schema(description = "Whether page is publicly visible")
    private Boolean published;
}

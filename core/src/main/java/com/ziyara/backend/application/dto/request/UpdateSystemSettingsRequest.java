package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Partial update for platform settings")
public class UpdateSystemSettingsRequest {

    @Size(max = 200)
    @Schema(description = "Public company / product display name")
    private String companyDisplayName;

    @Size(min = 3, max = 3)
    @Schema(description = "Default currency code (3 letters, e.g. USD, SAR)")
    private String defaultCurrency;

    @Schema(description = "When true, clients may show a maintenance banner (interpreted by frontends)")
    private Boolean maintenanceMode;

    @Schema(description = "When true, the provider portal is locked for maintenance (providers cannot log in or submit data)")
    private Boolean providerMaintenanceMode;
}

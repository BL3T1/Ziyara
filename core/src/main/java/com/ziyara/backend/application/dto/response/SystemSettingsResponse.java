package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Merged platform settings (defaults + stored overrides)")
public class SystemSettingsResponse {

    private String companyDisplayName;
    private String defaultCurrency;
    private boolean maintenanceMode;
    private boolean providerMaintenanceMode;
}

package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to add taxi to a booking")
public class AddTaxiRequest {

    @NotBlank
    @Schema(description = "Pickup location", required = true)
    private String pickupLocation;

    @NotBlank
    @Schema(description = "Destination location", required = true)
    private String destinationLocation;

    @Schema(description = "Vehicle type")
    private VehicleType vehicleType;

    @Schema(description = "Scheduled time")
    private LocalDateTime scheduledAt;
}

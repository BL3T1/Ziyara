package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dashboard: service counts per vertical and optional active bookings per type")
public class ServiceHealthResponse {

    @Schema(description = "Service count per type (e.g. HOTEL, TAXI, RESTAURANT)")
    private Map<String, Long> serviceCountByType;

    @Schema(description = "Active booking count per service type")
    private Map<String, Long> activeBookingCountByType;
}

package com.ziyara.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryLocationResponse {
    private UUID bookingId;
    private Double latitude;
    private Double longitude;
    private String status;
    private String updatedAt;
}

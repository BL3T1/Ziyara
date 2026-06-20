package com.ziyara.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound WebSocket message — broadcast to all subscribers on
 * {@code /topic/tracking/{bookingId}} whenever the driver pushes a new location.
 */
@Data
@AllArgsConstructor
public class TaxiLocationBroadcast {

    private UUID bookingId;
    private double latitude;
    private double longitude;
    private Instant timestamp;
}

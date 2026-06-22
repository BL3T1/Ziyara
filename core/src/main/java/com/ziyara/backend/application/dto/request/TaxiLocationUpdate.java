package com.ziyara.backend.application.dto.request;

import lombok.Data;

/**
 * Inbound WebSocket payload — driver pushes their current GPS coordinates.
 * Received on STOMP destination {@code /app/taxi/location/{bookingId}}.
 */
@Data
public class TaxiLocationUpdate {

    private double latitude;
    private double longitude;
}

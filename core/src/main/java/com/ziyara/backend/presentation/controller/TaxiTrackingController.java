package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.request.TaxiLocationUpdate;
import com.ziyara.backend.application.dto.response.TaxiLocationBroadcast;
import com.ziyara.backend.application.service.TaxiBookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * WebSocket/STOMP controller for live taxi driver tracking.
 *
 * <h2>Client flow</h2>
 * <ol>
 *   <li>Connect to STOMP endpoint {@code /ws} (SockJS fallback available).</li>
 *   <li>Subscribe to {@code /topic/tracking/{bookingId}} to receive location updates.</li>
 *   <li>Driver sends location via {@code /app/taxi/location/{bookingId}}.</li>
 * </ol>
 *
 * <h2>Security</h2>
 * <ul>
 *   <li>The driver endpoint verifies the caller is the assigned driver via
 *       {@link TaxiBookingService#assertIsDriver}.</li>
 *   <li>The admin endpoint is restricted to {@code SUPER_ADMIN}.</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@Tag(name = "Taxi Tracking", description = "Live driver location over STOMP/WebSocket")
public class TaxiTrackingController {

    private final SimpMessagingTemplate broker;
    private final TaxiBookingService taxiBookingService;

    /**
     * Driver pushes their current GPS coordinates.
     * Subscribers on {@code /topic/tracking/{bookingId}} receive a {@link TaxiLocationBroadcast}.
     */
    @MessageMapping("/taxi/location/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public void driverLocation(@DestinationVariable UUID bookingId,
                               @Payload TaxiLocationUpdate update,
                               Principal principal) {
        UUID driverId = UUID.fromString(principal.getName());
        taxiBookingService.assertIsDriver(bookingId, driverId);
        broker.convertAndSend(
                "/topic/tracking/" + bookingId,
                new TaxiLocationBroadcast(bookingId, update.getLatitude(), update.getLongitude(), Instant.now()));
    }

    /**
     * Admin override — push any location for testing / ops purposes.
     * Restricted to {@code SUPER_ADMIN}.
     */
    @MessageMapping("/taxi/location/{bookingId}/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void adminPushLocation(@DestinationVariable UUID bookingId,
                                  @Payload TaxiLocationUpdate update) {
        broker.convertAndSend(
                "/topic/tracking/" + bookingId,
                new TaxiLocationBroadcast(bookingId, update.getLatitude(), update.getLongitude(), Instant.now()));
    }
}

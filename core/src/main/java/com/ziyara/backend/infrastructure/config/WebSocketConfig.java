package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.infrastructure.config.properties.ZiyaraCorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket/STOMP configuration for real-time features:
 * - In-app notification push (replaces polling)
 * - Live driver tracking broadcast for taxi bookings
 *
 * Clients connect to /ws with SockJS fallback, then subscribe to:
 *   /topic/notifications/{userId}  — personal notification stream
 *   /topic/tracking/{bookingId}    — live driver location for taxi bookings
 *
 * Allowed origins are read from the same {@code ZiyaraCorsProperties} bean used by
 * {@link SecurityConfig}, ensuring WebSocket and HTTP CORS policy stay in sync.
 * Set {@code ZIYARA_CORS_ALLOWED_ORIGINS} in production.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ZiyaraCorsProperties corsProperties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-memory broker for topics and queues
        registry.enableSimpleBroker("/topic", "/queue");
        // Application destination prefix for @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        // User destination prefix for point-to-point messages
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> origins = corsProperties.resolveAllowedOrigins();
        // Fall back to a non-wildcard safe default rather than allowing all origins
        if (origins.isEmpty()) {
            origins = List.of("http://localhost");
        }
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins.toArray(new String[0]))
                .withSockJS();
    }
}

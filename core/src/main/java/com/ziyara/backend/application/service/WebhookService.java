package com.ziyara.backend.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.application.dto.request.CreateWebhookSubscriptionRequest;
import com.ziyara.backend.application.dto.response.WebhookDeliveryResponse;
import com.ziyara.backend.application.dto.response.WebhookSubscriptionResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.modules.webhook.api.WebhookEventPublisher;
import com.ziyara.backend.modules.webhook.api.WebhookRetryApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages outbound webhook subscriptions and dispatches signed event payloads
 * to subscriber endpoints after the originating transaction commits.
 */
@Service
@Slf4j
public class WebhookService implements WebhookEventPublisher, WebhookRetryApi {

    private static final List<String> SUPPORTED_EVENTS =
            List.of("booking.created", "content.approved", "payout.processed");

    private final JdbcTemplate jdbc;
    private final TaskExecutor asyncExecutor;
    private final ObjectMapper objectMapper;
    private final RestTemplate webhookRestTemplate;

    public WebhookService(JdbcTemplate jdbc,
                          @org.springframework.beans.factory.annotation.Qualifier("taskExecutor") TaskExecutor asyncExecutor,
                          ObjectMapper objectMapper,
                          RestTemplate webhookRestTemplate) {
        this.jdbc = jdbc;
        this.asyncExecutor = asyncExecutor;
        this.objectMapper = objectMapper;
        this.webhookRestTemplate = webhookRestTemplate;
    }

    // ── WebhookEventPublisher ─────────────────────────────────────────────────

    @Override
    public void publishAfterCommit(String event, Map<String, Object> payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    asyncExecutor.execute(() -> dispatchEvent(event, payload));
                }
            });
        } else {
            asyncExecutor.execute(() -> dispatchEvent(event, payload));
        }
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    private void dispatchEvent(String event, Map<String, Object> data) {
        String eventFilter = "[\"" + event + "\"]";
        List<Map<String, Object>> subs;
        try {
            subs = jdbc.queryForList(
                    "SELECT id::text, url, secret FROM webhook_subscriptions " +
                    "WHERE active = TRUE AND events @> ?::jsonb",
                    eventFilter
            );
        } catch (Exception ex) {
            log.warn("webhook: subscription query failed for event '{}': {}", event, ex.getMessage());
            return;
        }

        if (subs.isEmpty()) return;

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", UUID.randomUUID().toString());
        envelope.put("event", event);
        envelope.put("timestamp", Instant.now().toString());
        envelope.put("data", data);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            log.error("webhook: failed to serialize payload for event '{}'", event, ex);
            return;
        }

        for (Map<String, Object> sub : subs) {
            UUID subId = UUID.fromString((String) sub.get("id"));
            String url = (String) sub.get("url");
            String secret = (String) sub.get("secret");
            doDeliver(subId, event, url, secret, payloadJson);
        }
    }

    private void doDeliver(UUID subId, String event, String url, String secret, String payloadJson) {
        UUID deliveryId = UUID.randomUUID();
        Instant now = Instant.now();

        // Insert delivery log as PENDING
        jdbc.update(
                "INSERT INTO webhook_deliveries (id, subscription_id, event, payload, status, attempt_count, created_at) " +
                "VALUES (?, ?, ?, ?, 'PENDING', 0, ?)",
                deliveryId, subId, event, payloadJson, Timestamp.from(now)
        );

        try {
            String signature = "sha256=" + hmacSha256(secret, payloadJson);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Ziyara-Signature", signature);
            headers.set("X-Ziyara-Event", event);
            headers.set("X-Ziyara-Delivery", deliveryId.toString());

            var response = webhookRestTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(payloadJson, headers),
                    String.class
            );

            int httpStatus = response.getStatusCode().value();
            String respBody = response.getBody();
            boolean success = httpStatus >= 200 && httpStatus < 300;

            jdbc.update(
                    "UPDATE webhook_deliveries SET status = ?, http_status = ?, response_body = ?, " +
                    "attempt_count = 1, last_attempt_at = ? WHERE id = ?",
                    success ? "DELIVERED" : "FAILED",
                    httpStatus,
                    respBody != null && respBody.length() > 500 ? respBody.substring(0, 500) : respBody,
                    Timestamp.from(Instant.now()),
                    deliveryId
            );
            log.info("webhook: {} → {} [{}] delivery={}", event, url, httpStatus, deliveryId);

        } catch (Exception ex) {
            jdbc.update(
                    "UPDATE webhook_deliveries SET status = 'FAILED', response_body = ?, " +
                    "attempt_count = 1, last_attempt_at = ? WHERE id = ?",
                    ex.getMessage() != null ? ex.getMessage().substring(0, Math.min(500, ex.getMessage().length())) : "error",
                    Timestamp.from(Instant.now()),
                    deliveryId
            );
            log.warn("webhook: {} → {} FAILED delivery={}: {}", event, url, deliveryId, ex.getMessage());
        }
    }

    public List<String> getSupportedEvents() {
        return SUPPORTED_EVENTS;
    }

    // ── Retry failed deliveries ───────────────────────────────────────────────

    @Transactional
    public int retryFailedDeliveries() {
        List<Map<String, Object>> failed = jdbc.queryForList(
                "SELECT d.id::text, d.event, d.payload, d.attempt_count, " +
                "       s.url, s.secret " +
                "FROM webhook_deliveries d " +
                "JOIN webhook_subscriptions s ON s.id = d.subscription_id " +
                "WHERE d.status = 'FAILED' AND d.attempt_count < 3 AND s.active = TRUE " +
                "ORDER BY d.last_attempt_at ASC NULLS FIRST " +
                "LIMIT 50"
        );
        int retried = 0;
        for (Map<String, Object> row : failed) {
            UUID deliveryId = UUID.fromString((String) row.get("id"));
            String event     = (String) row.get("event");
            String payload   = (String) row.get("payload");
            String url       = (String) row.get("url");
            String secret    = (String) row.get("secret");
            int attemptCount = ((Number) row.get("attempt_count")).intValue();
            try {
                String signature = "sha256=" + hmacSha256(secret, payload);
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                headers.set("X-Ziyara-Signature", signature);
                headers.set("X-Ziyara-Event", event);
                headers.set("X-Ziyara-Delivery", deliveryId.toString());
                var response = webhookRestTemplate.exchange(
                        url, HttpMethod.POST,
                        new HttpEntity<>(payload, headers),
                        String.class
                );
                int httpStatus = response.getStatusCode().value();
                boolean success = httpStatus >= 200 && httpStatus < 300;
                int newCount = attemptCount + 1;
                String newStatus = success ? "DELIVERED" : (newCount >= 3 ? "PERMANENTLY_FAILED" : "FAILED");
                jdbc.update(
                        "UPDATE webhook_deliveries SET status = ?, http_status = ?, response_body = ?, " +
                        "attempt_count = ?, last_attempt_at = ? WHERE id = ?",
                        newStatus, httpStatus,
                        response.getBody() != null && response.getBody().length() > 500 ? response.getBody().substring(0, 500) : response.getBody(),
                        newCount, Timestamp.from(Instant.now()), deliveryId
                );
                log.info("webhook retry: {} → {} [{}] delivery={} attempt={}", event, url, httpStatus, deliveryId, newCount);
            } catch (Exception ex) {
                int newCount = attemptCount + 1;
                String newStatus = newCount >= 3 ? "PERMANENTLY_FAILED" : "FAILED";
                jdbc.update(
                        "UPDATE webhook_deliveries SET status = ?, attempt_count = ?, last_attempt_at = ?, response_body = ? WHERE id = ?",
                        newStatus, newCount, Timestamp.from(Instant.now()),
                        ex.getMessage() != null ? ex.getMessage().substring(0, Math.min(500, ex.getMessage().length())) : "error",
                        deliveryId
                );
                log.warn("webhook retry failed: {} → {} delivery={} attempt={}: {}", event, url, deliveryId, newCount, ex.getMessage());
            }
            retried++;
        }
        return retried;
    }

    // ── Subscription CRUD ─────────────────────────────────────────────────────

    @Transactional
    public WebhookSubscriptionResponse create(CreateWebhookSubscriptionRequest req) {
        for (String event : req.getEvents()) {
            if (!SUPPORTED_EVENTS.contains(event)) {
                throw new IllegalArgumentException("Unsupported event: " + event + ". Supported: " + SUPPORTED_EVENTS);
            }
        }
        UUID id = UUID.randomUUID();
        String secret = generateSecret();
        String eventsJson;
        try {
            eventsJson = objectMapper.writeValueAsString(req.getEvents());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize events list", e);
        }
        jdbc.update(
                "INSERT INTO webhook_subscriptions (id, provider_id, name, url, events, secret, active, created_at) " +
                "VALUES (?, ?, ?, ?, ?::jsonb, ?, TRUE, CURRENT_TIMESTAMP)",
                id, req.getProviderId(), req.getName(), req.getUrl(), eventsJson, secret
        );
        return WebhookSubscriptionResponse.builder()
                .id(id)
                .providerId(req.getProviderId())
                .name(req.getName())
                .url(req.getUrl())
                .events(req.getEvents())
                .active(true)
                .createdAt(Instant.now())
                .secret(secret)   // returned once only
                .build();
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscriptionResponse> list(int page, int size) {
        int offset = page * size;
        return jdbc.query(
                "SELECT id::text, provider_id::text, name, url, events::text, active, created_at " +
                "FROM webhook_subscriptions ORDER BY created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> WebhookSubscriptionResponse.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .providerId(rs.getString("provider_id") != null ? UUID.fromString(rs.getString("provider_id")) : null)
                        .name(rs.getString("name"))
                        .url(rs.getString("url"))
                        .events(parseJsonArray(rs.getString("events")))
                        .active(rs.getBoolean("active"))
                        .createdAt(rs.getTimestamp("created_at").toInstant())
                        .build(),
                size, offset
        );
    }

    @Transactional
    public void delete(UUID id) {
        int rows = jdbc.update("DELETE FROM webhook_subscriptions WHERE id = ?", id);
        if (rows == 0) throw new ResourceNotFoundException("Webhook subscription not found");
    }

    @Transactional
    public void setActive(UUID id, boolean active) {
        int rows = jdbc.update(
                "UPDATE webhook_subscriptions SET active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                active, id
        );
        if (rows == 0) throw new ResourceNotFoundException("Webhook subscription not found");
    }

    public void ping(UUID id) {
        var row = jdbc.queryForList(
                "SELECT url, secret FROM webhook_subscriptions WHERE id = ?", id
        );
        if (row.isEmpty()) throw new ResourceNotFoundException("Webhook subscription not found");
        String url = (String) row.get(0).get("url");
        String secret = (String) row.get(0).get("secret");
        Map<String, Object> testData = Map.of("message", "Ziyara webhook ping", "subscriptionId", id.toString());
        doDeliver(id, "ping", url, secret, buildPingJson(id, testData));
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryResponse> listDeliveries(UUID subscriptionId, int page, int size) {
        int offset = page * size;
        return jdbc.query(
                "SELECT id::text, subscription_id::text, event, status, http_status, " +
                "attempt_count, last_attempt_at, created_at " +
                "FROM webhook_deliveries WHERE subscription_id = ? " +
                "ORDER BY created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> WebhookDeliveryResponse.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .subscriptionId(UUID.fromString(rs.getString("subscription_id")))
                        .event(rs.getString("event"))
                        .status(rs.getString("status"))
                        .httpStatus(rs.getObject("http_status", Integer.class))
                        .attemptCount(rs.getInt("attempt_count"))
                        .lastAttemptAt(rs.getTimestamp("last_attempt_at") != null ? rs.getTimestamp("last_attempt_at").toInstant() : null)
                        .createdAt(rs.getTimestamp("created_at").toInstant())
                        .build(),
                subscriptionId, size, offset
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Hex.encodeHexString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String buildPingJson(UUID subId, Map<String, Object> data) {
        try {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("id", UUID.randomUUID().toString());
            envelope.put("event", "ping");
            envelope.put("timestamp", Instant.now().toString());
            envelope.put("data", data);
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}

package com.ziyara.backend.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.application.dto.request.CreateWebhookSubscriptionRequest;
import com.ziyara.backend.application.dto.response.WebhookDeliveryResponse;
import com.ziyara.backend.application.dto.response.WebhookSubscriptionResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.WebhookDelivery;
import com.ziyara.backend.domain.entity.WebhookRetryTask;
import com.ziyara.backend.domain.entity.WebhookSubscription;
import com.ziyara.backend.domain.repository.WebhookDeliveryRepository;
import com.ziyara.backend.domain.repository.WebhookSubscriptionRepository;
import com.ziyara.backend.modules.webhook.api.WebhookEventPublisher;
import com.ziyara.backend.modules.webhook.api.WebhookRetryApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
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

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final TaskExecutor asyncExecutor;
    private final ObjectMapper objectMapper;
    private final RestTemplate webhookRestTemplate;

    public WebhookService(WebhookSubscriptionRepository subscriptionRepository,
                          WebhookDeliveryRepository deliveryRepository,
                          @org.springframework.beans.factory.annotation.Qualifier("taskExecutor") TaskExecutor asyncExecutor,
                          ObjectMapper objectMapper,
                          RestTemplate webhookRestTemplate) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
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
        List<WebhookSubscription> subs = subscriptionRepository.findActiveByEvent(event);
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

        for (WebhookSubscription sub : subs) {
            doDeliver(sub.getId(), event, sub.getUrl(), sub.getSecret(), payloadJson);
        }
    }

    private void doDeliver(UUID subId, String event, String url, String secret, String payloadJson) {
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setId(UUID.randomUUID());
        delivery.setSubscriptionId(subId);
        delivery.setEvent(event);
        delivery.setPayload(payloadJson);
        delivery.setStatus("PENDING");
        delivery.setAttemptCount(0);
        delivery.setCreatedAt(Instant.now());
        deliveryRepository.insert(delivery);

        try {
            String signature = "sha256=" + hmacSha256(secret, payloadJson);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Ziyara-Signature", signature);
            headers.set("X-Ziyara-Event", event);
            headers.set("X-Ziyara-Delivery", delivery.getId().toString());

            var response = webhookRestTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(payloadJson, headers),
                    String.class
            );

            int httpStatus = response.getStatusCode().value();
            String respBody = response.getBody();
            boolean success = httpStatus >= 200 && httpStatus < 300;

            delivery.setStatus(success ? "DELIVERED" : "FAILED");
            delivery.setHttpStatus(httpStatus);
            delivery.setResponseBody(respBody != null && respBody.length() > 500 ? respBody.substring(0, 500) : respBody);
            delivery.setAttemptCount(1);
            delivery.setLastAttemptAt(Instant.now());
            deliveryRepository.update(delivery);
            log.info("webhook: {} → {} [{}] delivery={}", event, url, httpStatus, delivery.getId());

        } catch (Exception ex) {
            delivery.setStatus("FAILED");
            delivery.setResponseBody(truncate(ex.getMessage(), 500));
            delivery.setAttemptCount(1);
            delivery.setLastAttemptAt(Instant.now());
            deliveryRepository.update(delivery);
            log.warn("webhook: {} → {} FAILED delivery={}: {}", event, url, delivery.getId(), ex.getMessage());
        }
    }

    public List<String> getSupportedEvents() {
        return SUPPORTED_EVENTS;
    }

    // ── Retry failed deliveries ───────────────────────────────────────────────

    @Transactional
    public int retryFailedDeliveries() {
        List<WebhookRetryTask> tasks = deliveryRepository.findFailedForRetry(50);
        int retried = 0;
        for (WebhookRetryTask task : tasks) {
            WebhookDelivery d = task.delivery();
            String url = task.url();
            String secret = task.secret();
            int attemptCount = d.getAttemptCount();
            try {
                String signature = "sha256=" + hmacSha256(secret, d.getPayload());
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                headers.set("X-Ziyara-Signature", signature);
                headers.set("X-Ziyara-Event", d.getEvent());
                headers.set("X-Ziyara-Delivery", d.getId().toString());
                var response = webhookRestTemplate.exchange(
                        url, HttpMethod.POST,
                        new HttpEntity<>(d.getPayload(), headers),
                        String.class
                );
                int httpStatus = response.getStatusCode().value();
                boolean success = httpStatus >= 200 && httpStatus < 300;
                int newCount = attemptCount + 1;
                d.setStatus(success ? "DELIVERED" : (newCount >= 3 ? "PERMANENTLY_FAILED" : "FAILED"));
                d.setHttpStatus(httpStatus);
                d.setResponseBody(truncate(response.getBody(), 500));
                d.setAttemptCount(newCount);
                d.setLastAttemptAt(Instant.now());
                deliveryRepository.update(d);
                log.info("webhook retry: {} → {} [{}] delivery={} attempt={}", d.getEvent(), url, httpStatus, d.getId(), newCount);
            } catch (Exception ex) {
                int newCount = attemptCount + 1;
                d.setStatus(newCount >= 3 ? "PERMANENTLY_FAILED" : "FAILED");
                d.setAttemptCount(newCount);
                d.setLastAttemptAt(Instant.now());
                d.setResponseBody(truncate(ex.getMessage(), 500));
                deliveryRepository.update(d);
                log.warn("webhook retry failed: {} → {} delivery={} attempt={}: {}", d.getEvent(), url, d.getId(), newCount, ex.getMessage());
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
        WebhookSubscription sub = new WebhookSubscription();
        sub.setId(UUID.randomUUID());
        sub.setProviderId(req.getProviderId());
        sub.setName(req.getName());
        sub.setUrl(req.getUrl());
        sub.setEvents(req.getEvents());
        sub.setSecret(generateSecret());
        sub.setActive(true);
        sub.setCreatedAt(Instant.now());
        subscriptionRepository.save(sub);

        return WebhookSubscriptionResponse.builder()
                .id(sub.getId())
                .providerId(sub.getProviderId())
                .name(sub.getName())
                .url(sub.getUrl())
                .events(sub.getEvents())
                .active(true)
                .createdAt(sub.getCreatedAt())
                .secret(sub.getSecret())
                .build();
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscriptionResponse> list(int page, int size) {
        return subscriptionRepository.findAll(size, (long) page * size).stream()
                .map(sub -> WebhookSubscriptionResponse.builder()
                        .id(sub.getId())
                        .providerId(sub.getProviderId())
                        .name(sub.getName())
                        .url(sub.getUrl())
                        .events(sub.getEvents())
                        .active(sub.isActive())
                        .createdAt(sub.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void delete(UUID id) {
        subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook subscription not found"));
        subscriptionRepository.deleteById(id);
    }

    @Transactional
    public void setActive(UUID id, boolean active) {
        subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook subscription not found"));
        subscriptionRepository.setActive(id, active);
    }

    public void ping(UUID id) {
        WebhookSubscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook subscription not found"));
        Map<String, Object> testData = Map.of("message", "Ziyara webhook ping", "subscriptionId", id.toString());
        doDeliver(id, "ping", sub.getUrl(), sub.getSecret(), buildPingJson(id, testData));
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryResponse> listDeliveries(UUID subscriptionId, int page, int size) {
        return deliveryRepository.findBySubscriptionId(subscriptionId, size, (long) page * size).stream()
                .map(d -> WebhookDeliveryResponse.builder()
                        .id(d.getId())
                        .subscriptionId(d.getSubscriptionId())
                        .event(d.getEvent())
                        .status(d.getStatus())
                        .httpStatus(d.getHttpStatus())
                        .attemptCount(d.getAttemptCount())
                        .lastAttemptAt(d.getLastAttemptAt())
                        .createdAt(d.getCreatedAt())
                        .build())
                .toList();
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

    private static String truncate(String s, int maxLen) {
        if (s == null) return "error";
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
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

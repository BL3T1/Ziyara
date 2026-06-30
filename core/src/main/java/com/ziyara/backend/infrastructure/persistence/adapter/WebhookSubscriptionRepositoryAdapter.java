package com.ziyara.backend.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.domain.entity.WebhookSubscription;
import com.ziyara.backend.domain.repository.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookSubscriptionRepositoryAdapter implements WebhookSubscriptionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public WebhookSubscription save(WebhookSubscription sub) {
        if (sub.getId() == null) sub.setId(UUID.randomUUID());
        String eventsJson = serializeEvents(sub.getEvents());
        jdbcTemplate.update(
                "INSERT INTO webhook_subscriptions (id, provider_id, name, url, events, secret, active, created_at) " +
                "VALUES (?, ?, ?, ?, ?::jsonb, ?, TRUE, CURRENT_TIMESTAMP)",
                sub.getId(), sub.getProviderId(), sub.getName(), sub.getUrl(), eventsJson, sub.getSecret()
        );
        return sub;
    }

    @Override
    public Optional<WebhookSubscription> findById(UUID id) {
        List<WebhookSubscription> result = jdbcTemplate.query(
                "SELECT id::text, provider_id::text, name, url, events::text, secret, active, created_at " +
                "FROM webhook_subscriptions WHERE id = ?",
                (rs, rn) -> mapRow(rs), id);
        return result.stream().findFirst();
    }

    @Override
    public List<WebhookSubscription> findActiveByEvent(String event) {
        String filter = "[\"" + event + "\"]";
        try {
            return jdbcTemplate.query(
                    "SELECT id::text, provider_id::text, name, url, events::text, secret, active, created_at " +
                    "FROM webhook_subscriptions WHERE active = TRUE AND events @> ?::jsonb",
                    (rs, rn) -> mapRow(rs), filter);
        } catch (Exception ex) {
            log.warn("webhook: subscription query failed for event '{}': {}", event, ex.getMessage());
            return List.of();
        }
    }

    @Override
    public List<WebhookSubscription> findAll(int limit, long offset) {
        return jdbcTemplate.query(
                "SELECT id::text, provider_id::text, name, url, events::text, secret, active, created_at " +
                "FROM webhook_subscriptions ORDER BY created_at DESC LIMIT ? OFFSET ?",
                (rs, rn) -> mapRow(rs), limit, offset);
    }

    @Override
    public void deleteById(UUID id) {
        jdbcTemplate.update("DELETE FROM webhook_subscriptions WHERE id = ?", id);
    }

    @Override
    public void setActive(UUID id, boolean active) {
        jdbcTemplate.update(
                "UPDATE webhook_subscriptions SET active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                active, id);
    }

    private WebhookSubscription mapRow(ResultSet rs) throws SQLException {
        WebhookSubscription sub = new WebhookSubscription();
        sub.setId(UUID.fromString(rs.getString("id")));
        String pid = rs.getString("provider_id");
        if (pid != null) sub.setProviderId(UUID.fromString(pid));
        sub.setName(rs.getString("name"));
        sub.setUrl(rs.getString("url"));
        sub.setEvents(deserializeEvents(rs.getString("events")));
        sub.setSecret(rs.getString("secret"));
        sub.setActive(rs.getBoolean("active"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) sub.setCreatedAt(createdAt.toInstant());
        return sub;
    }

    private String serializeEvents(List<String> events) {
        try {
            return objectMapper.writeValueAsString(events != null ? events : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> deserializeEvents(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.WebhookDelivery;
import com.ziyara.backend.domain.entity.WebhookRetryTask;
import com.ziyara.backend.domain.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebhookDeliveryRepositoryAdapter implements WebhookDeliveryRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insert(WebhookDelivery d) {
        jdbcTemplate.update(
                "INSERT INTO webhook_deliveries (id, subscription_id, event, payload, status, attempt_count, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                d.getId(), d.getSubscriptionId(), d.getEvent(), d.getPayload(), d.getStatus(),
                d.getAttemptCount(), d.getCreatedAt() != null ? Timestamp.from(d.getCreatedAt()) : new Timestamp(System.currentTimeMillis())
        );
    }

    @Override
    public void update(WebhookDelivery d) {
        jdbcTemplate.update(
                "UPDATE webhook_deliveries SET status = ?, http_status = ?, response_body = ?, " +
                "attempt_count = ?, last_attempt_at = ? WHERE id = ?",
                d.getStatus(), d.getHttpStatus(), d.getResponseBody(), d.getAttemptCount(),
                d.getLastAttemptAt() != null ? Timestamp.from(d.getLastAttemptAt()) : null,
                d.getId()
        );
    }

    @Override
    public List<WebhookRetryTask> findFailedForRetry(int limit) {
        return jdbcTemplate.query(
                "SELECT d.id::text, d.subscription_id::text, d.event, d.payload, d.status, " +
                "d.http_status, d.response_body, d.attempt_count, d.last_attempt_at, d.created_at, " +
                "s.url AS sub_url, s.secret AS sub_secret " +
                "FROM webhook_deliveries d " +
                "JOIN webhook_subscriptions s ON s.id = d.subscription_id " +
                "WHERE d.status = 'FAILED' AND d.attempt_count < 3 AND s.active = TRUE " +
                "ORDER BY d.last_attempt_at ASC NULLS FIRST LIMIT ?",
                (rs, rn) -> new WebhookRetryTask(mapRow(rs), rs.getString("sub_url"), rs.getString("sub_secret")),
                limit
        );
    }

    @Override
    public List<WebhookDelivery> findBySubscriptionId(UUID subscriptionId, int limit, long offset) {
        return jdbcTemplate.query(
                "SELECT id::text, subscription_id::text, event, payload, status, http_status, " +
                "response_body, attempt_count, last_attempt_at, created_at " +
                "FROM webhook_deliveries WHERE subscription_id = ? " +
                "ORDER BY created_at DESC LIMIT ? OFFSET ?",
                (rs, rn) -> mapRow(rs),
                subscriptionId, limit, offset
        );
    }

    private WebhookDelivery mapRow(ResultSet rs) throws SQLException {
        WebhookDelivery d = new WebhookDelivery();
        d.setId(UUID.fromString(rs.getString("id")));
        d.setSubscriptionId(UUID.fromString(rs.getString("subscription_id")));
        d.setEvent(rs.getString("event"));
        d.setPayload(rs.getString("payload"));
        d.setStatus(rs.getString("status"));
        d.setHttpStatus(rs.getObject("http_status", Integer.class));
        d.setResponseBody(rs.getString("response_body"));
        d.setAttemptCount(rs.getInt("attempt_count"));
        Timestamp last = rs.getTimestamp("last_attempt_at");
        if (last != null) d.setLastAttemptAt(last.toInstant());
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) d.setCreatedAt(created.toInstant());
        return d;
    }
}

package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.repository.RateLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginRateLimitRepositoryAdapter implements RateLimitRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Integer incrementAndGet(String identifier, String identifierType, String endpoint,
                                   Instant windowStart, Instant windowEnd) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    INSERT INTO sys_rate_limit_counters
                      (id, identifier, identifier_type, endpoint, request_count, window_start, window_end)
                    VALUES (gen_random_uuid(), ?, ?, ?, 1, ?, ?)
                    ON CONFLICT (identifier, identifier_type, endpoint, window_start)
                    DO UPDATE SET request_count = sys_rate_limit_counters.request_count + 1
                    RETURNING request_count
                    """,
                    Integer.class,
                    identifier, identifierType, endpoint,
                    Timestamp.from(windowStart), Timestamp.from(windowEnd));
        } catch (DataAccessException e) {
            log.warn("Rate limit counter increment failed (failing open): {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteExpired() {
        try {
            jdbcTemplate.update(
                    "DELETE FROM sys_rate_limit_counters WHERE window_end < NOW() - INTERVAL '2 days'");
        } catch (DataAccessException e) {
            log.warn("Rate limit counter cleanup failed: {}", e.getMessage());
        }
    }
}

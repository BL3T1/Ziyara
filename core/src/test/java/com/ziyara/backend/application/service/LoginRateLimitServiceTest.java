package com.ziyara.backend.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitServiceTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock ObjectProvider<StringRedisTemplate> redisProvider;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks LoginRateLimitService service;

    private static final String IP = "127.0.0.1";
    private static final String ENDPOINT = "POST:/auth/login";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "redisEnabled", false);
        ReflectionTestUtils.setField(service, "maxPerMinutePerIp", 5);
    }

    // ── Rate limiting disabled ───────────────────────────────────────────────

    @Test
    void disabled_alwaysAllows() {
        ReflectionTestUtils.setField(service, "enabled", false);
        assertThat(service.allow(IP, ENDPOINT)).isTrue();
        verifyNoInteractions(jdbcTemplate);
    }

    // ── Null / blank IP ──────────────────────────────────────────────────────

    @Test
    void nullIp_alwaysAllows() {
        assertThat(service.allow(null, ENDPOINT)).isTrue();
    }

    @Test
    void blankIp_alwaysAllows() {
        assertThat(service.allow("  ", ENDPOINT)).isTrue();
    }

    // ── Postgres path ────────────────────────────────────────────────────────

    @Test
    void postgresPath_underLimit_returnsTrue() {
        when(redisProvider.getIfAvailable()).thenReturn(null);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                .thenReturn(3);

        assertThat(service.allow(IP, ENDPOINT)).isTrue();
    }

    @Test
    void postgresPath_overLimit_returnsFalse() {
        when(redisProvider.getIfAvailable()).thenReturn(null);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                .thenReturn(6);

        assertThat(service.allow(IP, ENDPOINT)).isFalse();
    }

    @Test
    void postgresPath_dbThrows_allowsRequest() {
        when(redisProvider.getIfAvailable()).thenReturn(null);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any()))
                .thenThrow(new org.springframework.dao.DataAccessException("DB error") {});

        assertThat(service.allow(IP, ENDPOINT)).isTrue();
    }

    // ── Redis path ───────────────────────────────────────────────────────────

    @Test
    void redisPath_underLimit_returnsTrue() {
        ReflectionTestUtils.setField(service, "redisEnabled", true);
        when(redisProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(3L);

        assertThat(service.allow(IP, ENDPOINT)).isTrue();
    }

    @Test
    void redisPath_overLimit_returnsFalse() {
        ReflectionTestUtils.setField(service, "redisEnabled", true);
        when(redisProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(6L);

        assertThat(service.allow(IP, ENDPOINT)).isFalse();
    }

    @Test
    void redisPath_firstRequest_setsTtl() {
        ReflectionTestUtils.setField(service, "redisEnabled", true);
        when(redisProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        service.allow(IP, ENDPOINT);

        verify(redisTemplate).expire(anyString(), any());
    }
}

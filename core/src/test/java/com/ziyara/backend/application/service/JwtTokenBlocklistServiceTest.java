package com.ziyara.backend.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenBlocklistServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    JwtTokenBlocklistService service;
    JwtTokenBlocklistService serviceNoRedis;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new JwtTokenBlocklistService(redisTemplate);
        serviceNoRedis = new JwtTokenBlocklistService(null);
    }

    @Nested
    class RevokeUntilExpiry {

        @Test
        void nullJti_doesNothing() {
            service.revokeUntilExpiry(null, Instant.now().plusSeconds(3600));
            verifyNoInteractions(valueOps);
        }

        @Test
        void blankJti_doesNothing() {
            service.revokeUntilExpiry("  ", Instant.now().plusSeconds(3600));
            verifyNoInteractions(valueOps);
        }

        @Test
        void withRedis_storesKeyWithTtl() {
            String jti = "test-jti-001";
            Instant expiresAt = Instant.now().plusSeconds(3600);

            service.revokeUntilExpiry(jti, expiresAt);

            verify(valueOps).set(
                    eq("ziyara:jwt:revoked:" + jti),
                    eq("1"),
                    any(Duration.class)
            );
        }

        @Test
        void withoutRedis_storesInMemory() {
            String jti = "memory-jti-001";
            Instant expiresAt = Instant.now().plusSeconds(3600);

            serviceNoRedis.revokeUntilExpiry(jti, expiresAt);

            assertThat(serviceNoRedis.isRevoked(jti)).isTrue();
        }

        @Test
        void redisFails_fallsBackToMemory() {
            String jti = "fallback-jti";
            Instant expiresAt = Instant.now().plusSeconds(3600);
            doThrow(new RuntimeException("Redis down")).when(valueOps)
                    .set(anyString(), anyString(), any(Duration.class));

            service.revokeUntilExpiry(jti, expiresAt);

            assertThat(service.isRevoked(jti)).isTrue();
        }
    }

    @Nested
    class IsRevoked {

        @Test
        void nullJti_returnsFalse() {
            assertThat(service.isRevoked(null)).isFalse();
        }

        @Test
        void blankJti_returnsFalse() {
            assertThat(service.isRevoked("")).isFalse();
        }

        @Test
        void withRedis_keyPresent_returnsTrue() {
            when(redisTemplate.hasKey("ziyara:jwt:revoked:jti-abc")).thenReturn(Boolean.TRUE);
            assertThat(service.isRevoked("jti-abc")).isTrue();
        }

        @Test
        void withRedis_keyAbsent_returnsFalse() {
            when(redisTemplate.hasKey("ziyara:jwt:revoked:jti-xyz")).thenReturn(Boolean.FALSE);
            assertThat(service.isRevoked("jti-xyz")).isFalse();
        }

        @Test
        void redisFails_checksMemory_notPresent_returnsFalse() {
            when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis unavailable"));
            assertThat(service.isRevoked("unknown-jti")).isFalse();
        }

        @Test
        void withoutRedis_notRevoked_returnsFalse() {
            assertThat(serviceNoRedis.isRevoked("never-revoked")).isFalse();
        }

        @Test
        void withoutRedis_alreadyExpired_returnsFalse() {
            String jti = "expired-jti";
            Instant pastExpiry = Instant.now().minusSeconds(10);
            serviceNoRedis.revokeUntilExpiry(jti, pastExpiry);

            assertThat(serviceNoRedis.isRevoked(jti)).isFalse();
        }

        @Test
        void withoutRedis_notExpired_returnsTrue() {
            String jti = "valid-jti";
            Instant futureExpiry = Instant.now().plusSeconds(3600);
            serviceNoRedis.revokeUntilExpiry(jti, futureExpiry);

            assertThat(serviceNoRedis.isRevoked(jti)).isTrue();
        }
    }
}

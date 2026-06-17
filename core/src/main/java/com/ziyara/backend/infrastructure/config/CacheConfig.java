package com.ziyara.backend.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Shared Redis serializer with JavaTimeModule so LocalDate/LocalDateTime fields in
     * cached DTOs (ActivityFeedItemResponse.timestamp, CommissionAnalysisResponse.start,
     * PayoutSummaryResponse.start, etc.) round-trip correctly.
     *
     * Default typing (NON_FINAL + PROPERTY) adds "@class" to every value so Spring
     * can deserialize back to the correct concrete type without @JsonDeserialize hints.
     *
     * GenericJackson2JsonRedisSerializer's no-arg constructor creates a bare ObjectMapper
     * that lacks JavaTimeModule — serializing any LocalDateTime field throws:
     * "Java 8 date/time type not supported by default".
     */
    private static final GenericJackson2JsonRedisSerializer REDIS_SERIALIZER;
    static {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .build(),
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY);
        REDIS_SERIALIZER = new GenericJackson2JsonRedisSerializer(mapper);
    }

    private static RedisCacheConfiguration jsonCache(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(REDIS_SERIALIZER));
    }

    @Bean
    @Primary
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> redisConnectionFactory) {
        RedisConnectionFactory factory = redisConnectionFactory.getIfAvailable();
        if (factory != null) {
            return RedisCacheManager.builder(factory)
                    .cacheDefaults(jsonCache(Duration.ofMinutes(10)))
                    .withCacheConfiguration("staffRoleCatalog",      jsonCache(Duration.ofHours(1)))
                    .withCacheConfiguration("permissionCatalogue",    jsonCache(Duration.ofHours(1)))
                    .withCacheConfiguration("userPermissions",        jsonCache(Duration.ofMinutes(30)))
                    .withCacheConfiguration("providerStaffRole",      jsonCache(Duration.ofMinutes(30)))
                    .withCacheConfiguration("rolesCatalogue",         jsonCache(Duration.ofMinutes(30)))
                    .withCacheConfiguration("systemSettings",         jsonCache(Duration.ofMinutes(5)))
                    .withCacheConfiguration("exchangeRates",          jsonCache(Duration.ofHours(12)))
                    // Dashboard aggregation caches — prevents repeated full-table scans
                    // under concurrent admin load. TTLs chosen so the dashboard UI feels
                    // live while keeping DB query rates bounded.
                    .withCacheConfiguration("dashboardKpis",          jsonCache(Duration.ofMinutes(2)))
                    .withCacheConfiguration("dashboardActivity",       jsonCache(Duration.ofSeconds(30)))
                    .withCacheConfiguration("dashboardServiceHealth",  jsonCache(Duration.ofMinutes(2)))
                    .withCacheConfiguration("dashboardCommission",     jsonCache(Duration.ofMinutes(5)))
                    .withCacheConfiguration("dashboardPayouts",        jsonCache(Duration.ofMinutes(5)))
                    // Bootstrap/live assemble the above into a single response.
                    // Caching the assembled result eliminates the CompletableFuture
                    // executor overhead for all concurrent requests after the first.
                    .withCacheConfiguration("dashboardBootstrap",      jsonCache(Duration.ofSeconds(30)))
                    .withCacheConfiguration("dashboardLive",           jsonCache(Duration.ofSeconds(15)))
                    .withCacheConfiguration("serviceDetail",           jsonCache(Duration.ofSeconds(60)))
                    .transactionAware()
                    .build();
        }
        return new ConcurrentMapCacheManager(
                "staffRoleCatalog", "permissionCatalogue", "userPermissions", "providerStaffRole",
                "rolesCatalogue", "systemSettings", "exchangeRates",
                "dashboardKpis", "dashboardActivity", "dashboardServiceHealth",
                "dashboardCommission", "dashboardPayouts",
                "dashboardBootstrap", "dashboardLive", "serviceDetail");
    }

    /**
     * In-memory cache for UserPrincipal (Spring Security UserDetails).
     * UserPrincipal contains Collection<GrantedAuthority> which cannot be safely
     * round-tripped through GenericJackson2JsonRedisSerializer — keeping it local
     * avoids deserialization failures that silently break every authenticated request.
     *
     * Bounded by 5 000 entries and a 30-minute write TTL (matches
     * app.security.session-timeout-minutes) to prevent unbounded heap growth
     * as authenticated users accumulate. Explicit @CacheEvict calls in
     * CustomUserDetailsService.evictUserDetails() still work as before.
     */
    @Bean
    public CacheManager localCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("userDetails");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(Duration.ofMinutes(30)));
        return manager;
    }

    /**
     * In-memory Caffeine caches for service list and search results.
     * PageImpl is not safely Redis-serializable (no no-arg constructor), so these
     * stay local. Short TTL keeps the browsing/mobile hot path off the DB under load.
     */
    @Bean("servicesCacheManager")
    public CacheManager servicesCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("servicesList", "servicesSearch");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterWrite(Duration.ofSeconds(30)));
        return manager;
    }
}

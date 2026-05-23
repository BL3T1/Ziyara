package com.ziyara.backend.infrastructure.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
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
     * Use GenericJackson2JsonRedisSerializer so cached DTOs do not need to implement
     * Serializable. JdkSerializationRedisSerializer (the default) requires it and
     * throws SerializationFailedException for all Spring-data / Lombok-generated DTOs.
     */
    private static RedisCacheConfiguration jsonCache(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    @Primary
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> redisConnectionFactory) {
        RedisConnectionFactory factory = redisConnectionFactory.getIfAvailable();
        if (factory != null) {
            return RedisCacheManager.builder(factory)
                    .cacheDefaults(jsonCache(Duration.ofMinutes(10)))
                    .withCacheConfiguration("staffRoleCatalog",  jsonCache(Duration.ofHours(1)))
                    .withCacheConfiguration("permissionCatalogue", jsonCache(Duration.ofHours(1)))
                    .transactionAware()
                    .build();
        }
        return new ConcurrentMapCacheManager("staffRoleCatalog", "permissionCatalogue");
    }
}

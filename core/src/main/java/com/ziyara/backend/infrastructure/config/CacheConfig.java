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

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> redisConnectionFactory) {
        RedisConnectionFactory factory = redisConnectionFactory.getIfAvailable();
        if (factory != null) {
            RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10))
                    .disableCachingNullValues();

            RedisCacheConfiguration staffCatalog = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(1))
                    .disableCachingNullValues();

            RedisCacheConfiguration permissionCatalog = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(1))
                    .disableCachingNullValues();

            return RedisCacheManager.builder(factory)
                    .cacheDefaults(defaults)
                    .withCacheConfiguration("staffRoleCatalog", staffCatalog)
                    .withCacheConfiguration("permissionCatalogue", permissionCatalog)
                    .transactionAware()
                    .build();
        }
        return new ConcurrentMapCacheManager("staffRoleCatalog", "permissionCatalogue");
    }
}

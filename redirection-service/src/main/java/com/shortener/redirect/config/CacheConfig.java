package com.shortener.redirect.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shortener.common.CachedUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class CacheConfig {

    /** L1 in-process cache (W-TinyLFU admission) that absorbs viral links without a network hop. */
    @Bean
    Cache<String, CachedUrl> l1Cache(@Value("${shortener.cache.l1.max-size:50000}") long maxSize,
                                     @Value("${shortener.cache.l1.ttl-seconds:45}") long ttlSeconds) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .recordStats()
                .build();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

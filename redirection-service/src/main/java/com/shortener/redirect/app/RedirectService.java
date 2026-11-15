package com.shortener.redirect.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.shortener.common.CachedUrl;
import com.shortener.common.ShortenerExceptions.ShortUrlBlocked;
import com.shortener.common.ShortenerExceptions.ShortUrlExpired;
import com.shortener.common.ShortenerExceptions.ShortUrlNotFound;
import com.shortener.redirect.api.ClickContext;
import com.shortener.redirect.infra.ClickEventPublisher;
import com.shortener.redirect.infra.UrlMappingReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lookup order: L1 Caffeine -> Redis (cache-aside) -> Cassandra, with per-key single-flight on
 * misses, TTL jitter, negative caching and lazy expiry checks. Click events are fire-and-forget.
 */
@Service
public class RedirectService {

    private static final Logger log = LoggerFactory.getLogger(RedirectService.class);
    private static final Duration MAX_TTL = Duration.ofHours(24);
    private static final Duration NEGATIVE_TTL = Duration.ofSeconds(45);
    private static final Duration REDIS_TIMEOUT = Duration.ofMillis(8);

    private final Cache<String, CachedUrl> l1;
    private final ReactiveStringRedisTemplate redis;
    private final UrlMappingReader reader;
    private final ClickEventPublisher clicks;
    private final ObjectMapper mapper;
    private final Clock clock;

    /** Collapses concurrent cache misses for the same key on this pod into one DB read. */
    private final ConcurrentMap<String, Mono<CachedUrl>> inFlight = new ConcurrentHashMap<>();

    public RedirectService(Cache<String, CachedUrl> l1, ReactiveStringRedisTemplate redis, UrlMappingReader reader,
                           ClickEventPublisher clicks, ObjectMapper mapper, Clock clock) {
        this.l1 = l1;
        this.redis = redis;
        this.reader = reader;
        this.clicks = clicks;
        this.mapper = mapper;
        this.clock = clock;
    }

    public Mono<ResolvedUrl> resolve(String key, ClickContext ctx) {
        return lookup(key).flatMap(cached -> {
            if (cached.notFound()) {
                return Mono.error(new ShortUrlNotFound(key));
            }
            if (cached.expired(clock.instant())) {
                return Mono.error(new ShortUrlExpired(key));
            }
            if (cached.flagged()) {
                return Mono.error(new ShortUrlBlocked(key));
            }
            clicks.publish(key, ctx);                                  // never blocks, never fails the redirect
            return Mono.just(new ResolvedUrl(URI.create(cached.longUrl()), cached.permanent()));
        });
    }

    private Mono<CachedUrl> lookup(String key) {
        CachedUrl hit = l1.getIfPresent(key);
        if (hit != null) {
            return Mono.just(hit);
        }
        return redis.opsForValue().get(key)
                .timeout(REDIS_TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("Redis degraded for key={}: {}", key, ex.toString());
                    return Mono.empty();                               // fall through to Cassandra
                })
                .flatMap(this::parse)
                .doOnNext(v -> l1.put(key, v))
                .switchIfEmpty(Mono.defer(() -> loadSingleFlight(key)));
    }

    private Mono<CachedUrl> loadSingleFlight(String key) {
        return inFlight.computeIfAbsent(key, k ->
                reader.findByKey(k)
                        .defaultIfEmpty(CachedUrl.missing())
                        .flatMap(v -> populate(k, v))
                        .doFinally(sig -> inFlight.remove(k))
                        .cache());
    }

    private Mono<CachedUrl> populate(String key, CachedUrl value) {
        l1.put(key, value);
        Duration ttl = value.notFound() ? NEGATIVE_TTL : jitter(ttlFor(value));
        if (ttl.isNegative() || ttl.isZero()) {
            return Mono.just(value);
        }
        return serialize(value)
                .flatMap(json -> redis.opsForValue().set(key, json, ttl))
                .onErrorReturn(false)                                  // cache write failure is non-fatal
                .thenReturn(value);
    }

    private Duration ttlFor(CachedUrl v) {
        if (v.expiresAt() == null) return MAX_TTL;
        Duration remaining = Duration.between(clock.instant(), v.expiresAt());
        return remaining.compareTo(MAX_TTL) < 0 ? remaining : MAX_TTL;
    }

    private static Duration jitter(Duration d) {
        double factor = 0.9 + ThreadLocalRandom.current().nextDouble() * 0.2;   // +/-10% avoids synchronized expiry
        return Duration.ofMillis((long) (d.toMillis() * factor));
    }

    private Mono<CachedUrl> parse(String json) {
        try {
            return Mono.just(mapper.readValue(json, CachedUrl.class));
        } catch (JsonProcessingException e) {
            return Mono.empty();                                       // treat corrupt entries as a miss
        }
    }

    private Mono<String> serialize(CachedUrl v) {
        try {
            return Mono.just(mapper.writeValueAsString(v));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}

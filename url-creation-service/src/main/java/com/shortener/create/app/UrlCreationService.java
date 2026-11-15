package com.shortener.create.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortener.common.CachedUrl;
import com.shortener.common.ShortenerExceptions.AliasAlreadyExists;
import com.shortener.common.ShortenerExceptions.InvalidAlias;
import com.shortener.common.ShortenerExceptions.InvalidUrl;
import com.shortener.create.api.CreateUrlRequest;
import com.shortener.create.api.CreateUrlResponse;
import com.shortener.create.infra.UrlMappingWriter;
import com.shortener.create.infra.UrlMappingWriter.NewMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UrlCreationService {

    private static final Logger log = LoggerFactory.getLogger(UrlCreationService.class);
    private static final Pattern ALIAS = Pattern.compile("^[0-9a-zA-Z_-]{4,30}$");
    private static final Set<String> RESERVED = Set.of("api", "actuator", "admin", "health", "login", "static", "internal");

    private final KeyReservationService keys;
    private final UrlValidator validator;
    private final UrlMappingWriter writer;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final String baseUrl;

    public UrlCreationService(KeyReservationService keys, UrlValidator validator, UrlMappingWriter writer,
                              StringRedisTemplate redis, ObjectMapper mapper, Clock clock,
                              @Value("${shortener.base-url}") String baseUrl) {
        this.keys = keys;
        this.validator = validator;
        this.writer = writer;
        this.redis = redis;
        this.mapper = mapper;
        this.clock = clock;
        this.baseUrl = baseUrl;
    }

    public CreateUrlResponse create(CreateUrlRequest req, UUID userId) {
        validator.validate(req.longUrl());

        Instant now = clock.instant();
        if (req.expiresAt() != null && !req.expiresAt().isAfter(now)) {
            throw new InvalidUrl("expiresAt must be in the future.");
        }

        boolean custom = req.customAlias() != null && !req.customAlias().isBlank();
        String key;
        if (custom) {
            key = req.customAlias();
            if (!ALIAS.matcher(key).matches()) {
                throw new InvalidAlias("Alias must be 4-30 chars of [0-9a-zA-Z_-].");
            }
            if (RESERVED.contains(key.toLowerCase())) {
                throw new AliasAlreadyExists(key);
            }
        } else {
            key = keys.nextKey();
        }

        NewMapping mapping = new NewMapping(key, req.longUrl().trim(), userId, now, req.expiresAt(),
                req.permanent() ? 301 : 302, custom, 0);

        if (!writer.insertIfAbsent(mapping)) {                 // Cassandra LWT: INSERT ... IF NOT EXISTS
            if (custom) throw new AliasAlreadyExists(key);
            throw new IllegalStateException("Generated key collision (should be impossible): " + key);
        }
        warmCache(mapping);
        return new CreateUrlResponse(baseUrl + "/" + key, key, req.expiresAt());
    }

    /** Write-around warm-up so the first click is a cache hit. Failures here are non-fatal. */
    private void warmCache(NewMapping m) {
        try {
            CachedUrl value = new CachedUrl(m.longUrl(), m.expiresAt(), m.redirectType() == 301, false, false);
            Duration ttl = Duration.ofHours(1);
            if (m.expiresAt() != null) {
                Duration remaining = Duration.between(clock.instant(), m.expiresAt());
                if (remaining.compareTo(ttl) < 0) ttl = remaining;
            }
            if (!ttl.isNegative() && !ttl.isZero()) {
                redis.opsForValue().set(m.shortKey(), mapper.writeValueAsString(value), ttl);
            }
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Cache warm-up failed for {}: {}", m.shortKey(), e.toString());
        }
    }
}

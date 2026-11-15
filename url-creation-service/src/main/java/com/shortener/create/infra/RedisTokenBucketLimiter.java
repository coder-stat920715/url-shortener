package com.shortener.create.infra;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Distributed token bucket executed atomically in Redis via Lua (one key per principal, so it is
 * Redis-Cluster safe). Equivalent to Bucket4j's Redis integration without the extra dependency.
 */
@Component
public class RedisTokenBucketLimiter {

    public record Result(boolean allowed, long remaining, long retryAfterSeconds) {}

    private static final String LUA = """
            local capacity = tonumber(ARGV[1])
            local refill   = tonumber(ARGV[2])
            local now      = tonumber(ARGV[3])
            local data     = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
            local tokens   = tonumber(data[1])
            local ts       = tonumber(data[2])
            if tokens == nil then tokens = capacity; ts = now end
            local delta = math.max(0, now - ts) / 1000.0
            tokens = math.min(capacity, tokens + delta * refill)
            local allowed = 0
            if tokens >= 1 then tokens = tokens - 1; allowed = 1 end
            redis.call('HSET', KEYS[1], 'tokens', tostring(tokens), 'ts', tostring(now))
            redis.call('PEXPIRE', KEYS[1], math.ceil(capacity / refill * 1000) * 2)
            return {allowed, math.floor(tokens)}
            """;

    @SuppressWarnings({"rawtypes"})
    private final DefaultRedisScript<List> script = new DefaultRedisScript<>(LUA, List.class);

    private final StringRedisTemplate redis;

    public RedisTokenBucketLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @SuppressWarnings("unchecked")
    public Result tryConsume(String key, long capacity, double refillPerSecond) {
        List<Long> r = (List<Long>) redis.execute(script, List.of("rl:" + key),
                String.valueOf(capacity), String.valueOf(refillPerSecond), String.valueOf(System.currentTimeMillis()));
        boolean allowed = r != null && r.get(0) == 1L;
        long remaining = r == null ? 0 : r.get(1);
        long retry = allowed ? 0 : (long) Math.ceil(1.0 / refillPerSecond);
        return new Result(allowed, remaining, retry);
    }
}

package com.shortener.create.infra;

import com.shortener.common.ShortenerExceptions.RateLimitExceeded;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Per-user (or per-IP when anonymous) token-bucket rate limit for URL creation. Fails open if Redis is down. */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RedisTokenBucketLimiter limiter;
    private final long userBurst;
    private final long userPerHour;
    private final long anonBurst;
    private final long anonPerMinute;

    public RateLimitInterceptor(RedisTokenBucketLimiter limiter,
                                @Value("${shortener.rate-limit.user.burst:20}") long userBurst,
                                @Value("${shortener.rate-limit.user.per-hour:100}") long userPerHour,
                                @Value("${shortener.rate-limit.anonymous.burst:10}") long anonBurst,
                                @Value("${shortener.rate-limit.anonymous.per-minute:10}") long anonPerMinute) {
        this.limiter = limiter;
        this.userBurst = userBurst;
        this.userPerHour = userPerHour;
        this.anonBurst = anonBurst;
        this.anonPerMinute = anonPerMinute;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String userId = req.getHeader("X-User-Id");
        boolean anonymous = userId == null || userId.isBlank();
        String principal = anonymous ? "ip:" + clientIp(req) : "u:" + userId;
        long capacity = anonymous ? anonBurst : userBurst;
        double refill = anonymous ? anonPerMinute / 60.0 : userPerHour / 3600.0;

        RedisTokenBucketLimiter.Result result;
        try {
            result = limiter.tryConsume("create:" + principal, capacity, refill);
        } catch (Exception e) {
            log.warn("Rate limiter unavailable, failing open: {}", e.toString());
            return true;
        }
        res.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        if (!result.allowed()) {
            throw new RateLimitExceeded(result.retryAfterSeconds());
        }
        return true;
    }

    /** Only trust X-Forwarded-For when the service sits behind your own gateway/load balancer. */
    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}

package com.shortener.common;

import org.springframework.http.HttpStatus;

public final class ShortenerExceptions {

    private ShortenerExceptions() {}

    public static class ShortUrlNotFound extends ShortenerException {
        public ShortUrlNotFound(String key) {
            super(HttpStatus.NOT_FOUND, "short-url-not-found", "Short URL not found",
                    "No link exists for key '" + key + "'.");
        }
    }

    public static class ShortUrlExpired extends ShortenerException {
        public ShortUrlExpired(String key) {
            super(HttpStatus.GONE, "short-url-expired", "Short URL expired",
                    "The link '" + key + "' has expired and is no longer available.");
        }
    }

    public static class ShortUrlBlocked extends ShortenerException {
        public ShortUrlBlocked(String key) {
            super(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS, "short-url-blocked", "Short URL blocked",
                    "The link '" + key + "' was flagged as unsafe.");
        }
    }

    public static class AliasAlreadyExists extends ShortenerException {
        public AliasAlreadyExists(String alias) {
            super(HttpStatus.CONFLICT, "alias-taken", "Alias already in use",
                    "The alias '" + alias + "' is already taken. Choose another.");
        }
    }

    public static class InvalidAlias extends ShortenerException {
        public InvalidAlias(String detail) {
            super(HttpStatus.BAD_REQUEST, "invalid-alias", "Invalid alias", detail);
        }
    }

    public static class InvalidUrl extends ShortenerException {
        public InvalidUrl(String detail) {
            super(HttpStatus.BAD_REQUEST, "invalid-url", "Invalid URL", detail);
        }
    }

    public static class RateLimitExceeded extends ShortenerException {
        private final long retryAfterSeconds;

        public RateLimitExceeded(long retryAfterSeconds) {
            super(HttpStatus.TOO_MANY_REQUESTS, "rate-limit-exceeded", "Too many requests",
                    "Rate limit exceeded. Retry in " + retryAfterSeconds + "s.");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long retryAfterSeconds() { return retryAfterSeconds; }
    }

    public static class KeyExhausted extends ShortenerException {
        public KeyExhausted(String detail) {
            super(HttpStatus.SERVICE_UNAVAILABLE, "key-supply-unavailable", "Key supply unavailable", detail);
        }
    }
}

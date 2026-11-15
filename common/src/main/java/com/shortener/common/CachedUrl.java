package com.shortener.common;

import java.time.Instant;

/** Compact value stored in Redis / Caffeine for each short key. Shared by creation and redirection. */
public record CachedUrl(String longUrl, Instant expiresAt, boolean permanent, boolean flagged, boolean notFound) {

    public static CachedUrl missing() {
        return new CachedUrl(null, null, false, false, true);
    }

    public boolean expired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}

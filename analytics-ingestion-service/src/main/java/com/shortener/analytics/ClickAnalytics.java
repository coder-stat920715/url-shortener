package com.shortener.analytics;

import java.time.Instant;
import java.util.UUID;

/** Enriched row persisted to ClickHouse table {@code clicks_raw}. */
public record ClickAnalytics(
        UUID eventId,
        String shortKey,
        Instant eventTime,
        String ipHash,
        String country,
        String city,
        String device,
        String browser,
        String os,
        String referrer) {}

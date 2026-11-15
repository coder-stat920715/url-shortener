package com.shortener.common;

import java.time.Instant;
import java.util.UUID;

/** Raw click event published by the redirection service to Kafka topic {@code url-clicks}. */
public record ClickEventDTO(
        UUID eventId,
        String shortKey,
        Instant timestamp,
        String ipAddress,
        String userAgent,
        String referrer) {

    public static final String TOPIC = "url-clicks";
}

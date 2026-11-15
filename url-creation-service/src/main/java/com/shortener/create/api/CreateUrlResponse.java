package com.shortener.create.api;

import java.time.Instant;

public record CreateUrlResponse(String shortUrl, String shortKey, Instant expiresAt) {}

package com.shortener.create.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateUrlRequest(
        @NotBlank @Size(max = 2048) String longUrl,
        @Size(min = 4, max = 30) String customAlias,
        Instant expiresAt,
        boolean permanent) {}

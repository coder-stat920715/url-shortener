package com.shortener.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;

/** Builds RFC 7807 {@code application/problem+json} responses. Shared by MVC and WebFlux advices. */
public final class ProblemFactory {

    private static final String BASE = "https://short.url/problems/";

    private ProblemFactory() {}

    public static ResponseEntity<ProblemDetail> from(ShortenerException ex, String path) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(BASE + ex.type()));
        pd.setTitle(ex.title());
        pd.setInstance(URI.create(path));
        pd.setProperty("timestamp", Instant.now().toString());

        ResponseEntity.BodyBuilder b = ResponseEntity.status(ex.status()).contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (ex instanceof ShortenerExceptions.RateLimitExceeded rl) {
            pd.setProperty("retryAfterSeconds", rl.retryAfterSeconds());
            b.header(HttpHeaders.RETRY_AFTER, String.valueOf(rl.retryAfterSeconds()));
        }
        return b.body(pd);
    }

    public static ResponseEntity<ProblemDetail> badRequest(String detail, String path) {
        return custom(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request", detail, path);
    }

    public static ResponseEntity<ProblemDetail> internal(String path) {
        return custom(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal error",
                "An unexpected error occurred.", path);
    }

    private static ResponseEntity<ProblemDetail> custom(HttpStatus s, String type, String title, String detail, String path) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(s, detail);
        pd.setType(URI.create(BASE + type));
        pd.setTitle(title);
        pd.setInstance(URI.create(path));
        pd.setProperty("timestamp", Instant.now().toString());
        return ResponseEntity.status(s).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }
}

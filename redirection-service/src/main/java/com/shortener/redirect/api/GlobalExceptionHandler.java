package com.shortener.redirect.api;

import com.shortener.common.ProblemFactory;
import com.shortener.common.ShortenerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** RFC 7807 responses for 404, 410, 451 and unexpected errors on the redirect path. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ShortenerException.class)
    ResponseEntity<ProblemDetail> domain(ShortenerException ex, ServerHttpRequest req) {
        return ProblemFactory.from(ex, req.getPath().value());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex, ServerHttpRequest req) {
        log.error("Unhandled error on {}", req.getPath().value(), ex);
        return ProblemFactory.internal(req.getPath().value());
    }
}

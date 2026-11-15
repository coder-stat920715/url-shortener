package com.shortener.common;

import org.springframework.http.HttpStatus;

/** Base class for all domain errors; each maps to one RFC 7807 problem type. */
public abstract class ShortenerException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String title;

    protected ShortenerException(HttpStatus status, String type, String title, String detail) {
        super(detail);
        this.status = status;
        this.type = type;
        this.title = title;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String title() { return title; }
}

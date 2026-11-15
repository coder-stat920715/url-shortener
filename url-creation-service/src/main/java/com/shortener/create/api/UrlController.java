package com.shortener.create.api;

import com.shortener.create.app.UrlCreationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlCreationService service;

    public UrlController(UrlCreationService service) {
        this.service = service;
    }

    /**
     * {@code X-User-Id} is injected by the gateway after JWT validation. In this reference
     * implementation it is optional (anonymous creation is allowed with tighter rate limits).
     */
    @PostMapping
    public ResponseEntity<CreateUrlResponse> create(@Valid @RequestBody CreateUrlRequest request,
                                                    @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, userId));
    }
}

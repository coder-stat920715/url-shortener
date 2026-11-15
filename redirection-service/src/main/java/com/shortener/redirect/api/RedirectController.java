package com.shortener.redirect.api;

import com.shortener.redirect.app.RedirectService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class RedirectController {

    private final RedirectService service;

    public RedirectController(RedirectService service) {
        this.service = service;
    }

    /** The regex rejects junk keys before any cache or DB call (cheap abuse filter). */
    @GetMapping("/{shortKey:[0-9a-zA-Z_-]{4,30}}")
    public Mono<ResponseEntity<Void>> redirect(@PathVariable String shortKey, ServerHttpRequest request) {
        return service.resolve(shortKey, ClickContext.from(request))
                .map(target -> ResponseEntity
                        .status(target.permanent() ? HttpStatus.MOVED_PERMANENTLY : HttpStatus.FOUND)
                        .location(target.uri())
                        .cacheControl(target.permanent()
                                ? CacheControl.maxAge(Duration.ofDays(1)).cachePublic()
                                : CacheControl.noStore())
                        .<Void>build());
    }
}

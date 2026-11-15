package com.shortener.redirect.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetSocketAddress;

/** Request metadata captured for click analytics. */
public record ClickContext(String clientIp, String userAgent, String referrer) {

    public static ClickContext from(ServerHttpRequest req) {
        HttpHeaders h = req.getHeaders();
        String ip = null;
        String xff = h.getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            ip = xff.split(",")[0].trim();
        } else {
            InetSocketAddress remote = req.getRemoteAddress();
            if (remote != null && remote.getAddress() != null) {
                ip = remote.getAddress().getHostAddress();
            }
        }
        return new ClickContext(ip, h.getFirst(HttpHeaders.USER_AGENT), h.getFirst(HttpHeaders.REFERER));
    }
}

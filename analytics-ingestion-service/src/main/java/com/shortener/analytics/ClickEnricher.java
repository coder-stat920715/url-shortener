package com.shortener.analytics;

import com.shortener.common.ClickEventDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Enrichment happens here, off the redirect path. The UA parsing is intentionally simple; swap in
 * uap-java / ua-parser for production. Geo lookup is a hook: plug in MaxMind GeoLite2 (see README).
 */
@Component
public class ClickEnricher {

    private final String salt;

    public ClickEnricher(@Value("${analytics.ip-hash-salt:change-me}") String salt) {
        this.salt = salt;
    }

    public ClickAnalytics enrich(ClickEventDTO e) {
        String ua = e.userAgent() == null ? "" : e.userAgent();
        String lower = ua.toLowerCase(Locale.ROOT);
        boolean bot = lower.contains("bot") || lower.contains("crawl") || lower.contains("spider");

        return new ClickAnalytics(
                e.eventId(),
                e.shortKey(),
                e.timestamp(),
                hashIp(e.ipAddress()),
                "XX",                                       // TODO: MaxMind GeoLite2 country lookup
                "",
                bot ? "bot" : (lower.contains("mobi") ? "mobile" : "desktop"),
                browser(ua),
                os(ua),
                e.referrer() == null ? "" : e.referrer());
    }

    private String hashIp(String ip) {                      // raw IPs are never stored (privacy)
        if (ip == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest((salt + ip).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String browser(String ua) {
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Chrome/")) return "Chrome";
        if (ua.contains("Safari/")) return "Safari";
        return "Other";
    }

    private static String os(String ua) {
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Mac OS X")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        return "Other";
    }
}

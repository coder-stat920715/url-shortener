package com.shortener.create.app;

import com.shortener.common.ShortenerExceptions.InvalidUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Scheme check, SSRF guard (no private/loopback targets) and domain blocklist. */
@Component
public class UrlValidator {

    private final Set<String> blockedDomains;

    public UrlValidator(@Value("${shortener.blocked-domains:}") String blocked) {
        this.blockedDomains = Arrays.stream(blocked.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public void validate(String longUrl) {
        URI uri;
        try {
            uri = new URI(longUrl.trim());
        } catch (URISyntaxException e) {
            throw new InvalidUrl("Malformed URL.");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new InvalidUrl("Only http and https URLs are allowed.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidUrl("URL must contain a host.");
        }
        String h = host.toLowerCase(Locale.ROOT);
        for (String b : blockedDomains) {
            if (h.equals(b) || h.endsWith("." + b)) {
                throw new InvalidUrl("This domain is not allowed.");
            }
        }
        try {
            for (InetAddress a : InetAddress.getAllByName(h)) {
                if (a.isLoopbackAddress() || a.isAnyLocalAddress() || a.isSiteLocalAddress()
                        || a.isLinkLocalAddress() || a.isMulticastAddress()) {
                    throw new InvalidUrl("URLs pointing to private or local addresses are not allowed.");
                }
            }
        } catch (UnknownHostException e) {
            throw new InvalidUrl("Host could not be resolved.");
        }
    }
}

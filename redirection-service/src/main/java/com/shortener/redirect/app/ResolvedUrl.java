package com.shortener.redirect.app;

import java.net.URI;

public record ResolvedUrl(URI uri, boolean permanent) {}

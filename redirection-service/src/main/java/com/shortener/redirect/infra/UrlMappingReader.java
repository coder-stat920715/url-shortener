package com.shortener.redirect.infra;

import com.shortener.common.CachedUrl;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/** Single-partition read of the url_mapping table (LOCAL_ONE by driver profile for latency). */
@Repository
public class UrlMappingReader {

    private static final String CQL =
            "SELECT long_url, expires_at, redirect_type, status FROM url_mapping WHERE short_key = ?";

    private final ReactiveCassandraOperations cassandra;

    public UrlMappingReader(ReactiveCassandraOperations cassandra) {
        this.cassandra = cassandra;
    }

    public Mono<CachedUrl> findByKey(String key) {
        return cassandra.getReactiveCqlOperations()
                .query(CQL, (row, i) -> new CachedUrl(
                        row.getString("long_url"),
                        row.getInstant("expires_at"),
                        row.getInt("redirect_type") == 301,
                        row.getInt("status") == 2,
                        false), key)
                .next();
    }
}

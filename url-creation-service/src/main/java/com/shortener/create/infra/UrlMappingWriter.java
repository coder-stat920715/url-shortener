package com.shortener.create.infra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Repository
public class UrlMappingWriter {

    public record NewMapping(String shortKey, String longUrl, UUID userId, Instant createdAt,
                             Instant expiresAt, int redirectType, boolean custom, int status) {}

    private final CqlSession session;
    private PreparedStatement insertMapping;
    private PreparedStatement insertByUser;

    public UrlMappingWriter(CqlSession session) {
        this.session = session;
    }

    @PostConstruct
    void prepare() {
        insertMapping = session.prepare(
                "INSERT INTO url_mapping (short_key, long_url, user_id, created_at, expires_at, "
                        + "redirect_type, is_custom, status) VALUES (?,?,?,?,?,?,?,?) IF NOT EXISTS USING TTL ?");
        insertByUser = session.prepare(
                "INSERT INTO urls_by_user (user_id, created_at, short_key, long_url) VALUES (?,?,?,?) USING TTL ?");
    }

    /** Lightweight transaction: returns false if the key already exists. */
    public boolean insertIfAbsent(NewMapping m) {
        int ttl = ttlSeconds(m);
        boolean applied = session.execute(insertMapping.bind(
                        m.shortKey(), m.longUrl(), m.userId(), m.createdAt(), m.expiresAt(),
                        m.redirectType(), m.custom(), m.status(), ttl)
                .setConsistencyLevel(DefaultConsistencyLevel.LOCAL_QUORUM)
                .setSerialConsistencyLevel(DefaultConsistencyLevel.LOCAL_SERIAL)).wasApplied();

        if (applied && m.userId() != null) {
            session.execute(insertByUser.bind(m.userId(), m.createdAt(), m.shortKey(), m.longUrl(), ttl)
                    .setConsistencyLevel(DefaultConsistencyLevel.LOCAL_QUORUM));
        }
        return applied;
    }

    private static int ttlSeconds(NewMapping m) {
        if (m.expiresAt() == null) return 0;                    // 0 = no expiry
        long s = Duration.between(Instant.now(), m.expiresAt()).toSeconds();
        return (int) Math.max(1, Math.min(s, Integer.MAX_VALUE));
    }
}

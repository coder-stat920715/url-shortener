CREATE KEYSPACE IF NOT EXISTS shortener
  WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
-- Production: NetworkTopologyStrategy with {'dc1': 3, 'dc2': 3}

CREATE TABLE IF NOT EXISTS shortener.url_mapping (
    short_key     text PRIMARY KEY,
    long_url      text,
    user_id       uuid,
    created_at    timestamp,
    expires_at    timestamp,
    redirect_type int,          -- 301 or 302
    is_custom     boolean,
    status        int           -- 0 active, 1 disabled, 2 flagged
) WITH compaction = {'class': 'LeveledCompactionStrategy'};

CREATE TABLE IF NOT EXISTS shortener.urls_by_user (
    user_id    uuid,
    created_at timestamp,
    short_key  text,
    long_url   text,
    PRIMARY KEY ((user_id), created_at, short_key)
) WITH CLUSTERING ORDER BY (created_at DESC, short_key ASC);

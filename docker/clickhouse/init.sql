CREATE TABLE IF NOT EXISTS default.clicks_raw
(
    event_id   UUID,
    short_key  String,
    event_time DateTime64(3),
    ip_hash    String,
    country    LowCardinality(String),
    city       String,
    device     LowCardinality(String),
    browser    LowCardinality(String),
    os         LowCardinality(String),
    referrer   String
)
ENGINE = MergeTree
PARTITION BY toYYYYMMDD(event_time)
ORDER BY (short_key, event_time)
TTL toDateTime(event_time) + INTERVAL 13 MONTH;

CREATE TABLE IF NOT EXISTS default.clicks_by_minute
(
    short_key String,
    minute    DateTime,
    clicks    UInt64
)
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(minute)
ORDER BY (short_key, minute);

CREATE MATERIALIZED VIEW IF NOT EXISTS default.clicks_by_minute_mv TO default.clicks_by_minute AS
SELECT short_key, toStartOfMinute(event_time) AS minute, count() AS clicks
FROM default.clicks_raw
GROUP BY short_key, minute;

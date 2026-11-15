package com.shortener.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bulk-inserts through ClickHouse's HTTP interface using JSONEachRow. One large insert per Kafka
 * poll keeps MergeTree happy (many tiny inserts create too many parts).
 */
@Component
public class ClickHouseBatchWriter {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC);
    private static final String INSERT = "INSERT INTO clicks_raw FORMAT JSONEachRow";

    private final RestClient http;
    private final ObjectMapper mapper;

    public ClickHouseBatchWriter(@Value("${analytics.clickhouse.url}") String url,
                                 @Value("${analytics.clickhouse.database:default}") String database,
                                 ObjectMapper mapper) {
        this.http = RestClient.builder().baseUrl(url + "/?database=" + database).build();
        this.mapper = mapper;
    }

    public void write(List<ClickAnalytics> rows) {
        if (rows.isEmpty()) return;
        StringBuilder body = new StringBuilder(rows.size() * 256);
        try {
            for (ClickAnalytics r : rows) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("event_id", r.eventId().toString());
                m.put("short_key", r.shortKey());
                m.put("event_time", TS.format(r.eventTime()));
                m.put("ip_hash", r.ipHash());
                m.put("country", r.country());
                m.put("city", r.city());
                m.put("device", r.device());
                m.put("browser", r.browser());
                m.put("os", r.os());
                m.put("referrer", r.referrer());
                body.append(mapper.writeValueAsString(m)).append('\n');
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize click rows", e);
        }
        http.post()
                .uri(b -> b.queryParam("query", INSERT).build())
                .body(body.toString())
                .retrieve()
                .toBodilessEntity();                                       // throws on non-2xx -> batch is retried
    }
}

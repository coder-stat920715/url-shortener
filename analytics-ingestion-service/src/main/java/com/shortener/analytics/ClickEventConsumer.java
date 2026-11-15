package com.shortener.analytics;

import com.shortener.common.ClickEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class ClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    private final ClickEnricher enricher;
    private final ClickHouseBatchWriter writer;

    public ClickEventConsumer(ClickEnricher enricher, ClickHouseBatchWriter writer) {
        this.enricher = enricher;
        this.writer = writer;
    }

    /**
     * Batch listener (listener.type=batch). Offsets are committed only after the insert succeeds
     * (at-least-once); duplicates on redelivery are tolerated by the dedupe-friendly event_id.
     */
    @KafkaListener(topics = ClickEventDTO.TOPIC, groupId = "analytics-ingestion")
    public void onBatch(List<ClickEventDTO> events) {
        List<ClickAnalytics> rows = events.stream()
                .filter(Objects::nonNull)
                .map(enricher::enrich)
                .toList();
        writer.write(rows);
        log.debug("Persisted {} click events", rows.size());
    }
}

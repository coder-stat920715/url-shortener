package com.shortener.redirect.infra;

import com.shortener.common.ClickEventDTO;
import com.shortener.redirect.api.ClickContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Fire-and-forget producer. With {@code max.block.ms=0} a full buffer or missing metadata fails
 * immediately instead of stalling the redirect. Analytics loss is acceptable; latency is not.
 */
@Component
public class ClickEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ClickEventPublisher.class);

    private final KafkaTemplate<String, ClickEventDTO> kafka;
    private final MeterRegistry metrics;
    private final Clock clock;

    public ClickEventPublisher(KafkaTemplate<String, ClickEventDTO> kafka, MeterRegistry metrics, Clock clock) {
        this.kafka = kafka;
        this.metrics = metrics;
        this.clock = clock;
    }

    public void publish(String shortKey, ClickContext ctx) {
        try {
            ClickEventDTO event = new ClickEventDTO(UUID.randomUUID(), shortKey, clock.instant(),
                    ctx.clientIp(), ctx.userAgent(), ctx.referrer());
            kafka.send(ClickEventDTO.TOPIC, shortKey, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    metrics.counter("clicks.publish.failed").increment();
                    log.debug("Click publish failed for {}: {}", shortKey, ex.toString());
                }
            });
        } catch (Exception e) {                                       // analytics must never break redirects
            metrics.counter("clicks.publish.dropped").increment();
        }
    }
}

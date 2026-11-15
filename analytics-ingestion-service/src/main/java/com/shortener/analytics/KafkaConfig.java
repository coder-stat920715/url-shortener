package com.shortener.analytics;

import com.shortener.common.ClickEventDTO;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
class KafkaConfig {

    /** In production use 48 partitions and replicas=3 (min.insync.replicas=2). */
    @Bean
    NewTopic clicksTopic() {
        return TopicBuilder.name(ClickEventDTO.TOPIC).partitions(12).replicas(1).build();
    }

    /** Retry a failed batch 3 times, 500 ms apart, then log and skip it (add a DLT recoverer for prod). */
    @Bean
    DefaultErrorHandler errorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(500L, 3));
    }
}

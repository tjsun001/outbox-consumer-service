package com.thurman.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.kafka.listener.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class KafkaOutboxConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxConsumer.class);

    @KafkaListener(
            topics = "${app.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(String value) {
        log.info("Consumed message: {}", value);
    }
}
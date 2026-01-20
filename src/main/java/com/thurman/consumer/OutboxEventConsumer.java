package com.thurman.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;

    @KafkaListener(
            topics = "${KAFKA_TOPIC_OUTBOX:outbox-events}",
            groupId = "${KAFKA_CONSUMER_GROUP_ID:product-consumer-v1}"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        String key = record.key();
        String value = record.value();

        // Parse message
        OutboxEventMessage msg = objectMapper.readValue(value, OutboxEventMessage.class);

        // Idempotency: if already processed, ack and return
        if (processedEventService.isAlreadyProcessed(msg.id())) {
            log.info("Skipping already-processed event id={} topic={} partition={} offset={}",
                    msg.id(), record.topic(), record.partition(), record.offset());
            ack.acknowledge();
            return;
        }

        try {
            // Do the "work"
            processedEventService.process(msg);

            // Only ack AFTER successful processing
            ack.acknowledge();
        } catch (Exception e) {
            // No ack => message will be re-delivered (at-least-once)
            log.error("Failed processing event id={} (will retry). topic={} partition={} offset={}",
                    msg.id(), record.topic(), record.partition(), record.offset(), e);
            throw e;
        }
    }
}

package com.thurman.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OutboxLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(OutboxLogConsumer.class);

    @KafkaListener(
            topics = "${KAFKA_TOPIC_OUTBOX:outbox.events.test}",
            groupId = "${KAFKA_CONSUMER_GROUP_ID:outbox-consumer-service-v1}"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {

        log.info(
                "RECEIVED topic={} partition={} offset={} key={} value={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value()
        );

        ack.acknowledge();
    }
}
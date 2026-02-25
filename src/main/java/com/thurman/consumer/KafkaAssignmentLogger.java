package com.thurman.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.Collection;

@Configuration
public class KafkaAssignmentLogger {

    private static final Logger log = LoggerFactory.getLogger(KafkaAssignmentLogger.class);

    @Bean
    public ConsumerRebalanceListener loggingRebalanceListener() {
        return new ConsumerRebalanceListener() {

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                log.info("PARTITIONS_REVOKED: {}", partitions);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                log.info("PARTITIONS_ASSIGNED: {}", partitions);
            }
        };
    }

    @Bean
    public Object attachRebalanceListener(
            KafkaListenerEndpointRegistry registry,
            ConsumerRebalanceListener listener
    ) {

        registry.getListenerContainers().forEach(container -> {
            if (container instanceof ConcurrentMessageListenerContainer<?, ?> c) {
                c.getContainerProperties().setConsumerRebalanceListener(listener);
            }
        });

        return new Object();
    }
}
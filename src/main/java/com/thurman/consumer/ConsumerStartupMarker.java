package com.thurman.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsumerStartupMarker {

    private static final Logger log = LoggerFactory.getLogger(ConsumerStartupMarker.class);

    @Value("${app.kafka.listener.enabled:false}")
    private boolean listenerEnabled;

    @Value("${app.topic:undefined}")
    private String topic;

    @Value("${spring.kafka.consumer.group-id:undefined}")
    private String groupId;

    @Value("${spring.kafka.consumer.enable-auto-commit:true}")
    private boolean autoCommit;

    @Bean
    ApplicationRunner consumerConfigOkMarker() {
        return args -> log.info(
                "CONSUMER_CONFIG_OK: enabled={} topic={} groupId={} autoCommit={}",
                listenerEnabled,
                topic,
                groupId,
                autoCommit
        );
    }
}
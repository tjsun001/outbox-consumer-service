package com.thurman.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.consumer.once.enabled",
        havingValue = "true"
)
public class KafkaConsumerOnceRunner implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaConsumerOnceRunner.class);

    private final Environment env;
    private final ConfigurableApplicationContext ctx;

    public KafkaConsumerOnceRunner(Environment env,
                                   ConfigurableApplicationContext ctx) {
        this.env = env;
        this.ctx = ctx;
    }

    @Override
    public void run(String... args) {

        String bootstrap =
                env.getRequiredProperty("spring.kafka.bootstrap-servers");

        String topic =
                env.getProperty("app.topic", "outbox.events.test");

        int pollSeconds =
                env.getProperty("app.consumer.once.poll-seconds",
                        Integer.class, 15);

        String groupId =
                "consume-once-" + UUID.randomUUID();

        log.info(
                "MODE=consume-once topic={} pollSeconds={} groupId={} bootstrap={}",
                topic, pollSeconds, groupId, bootstrap
        );

        Properties props = new Properties();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrap
        );

        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName()
        );

        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName()
        );

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId
        );

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        props.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                "true"
        );

        props.put("security.protocol", "SASL_SSL");

        props.put("sasl.mechanism", "AWS_MSK_IAM");

        props.put(
                "sasl.jaas.config",
                "software.amazon.msk.auth.iam.IAMLoginModule required;"
        );

        props.put(
                "sasl.client.callback.handler.class",
                "software.amazon.msk.auth.iam.IAMClientCallbackHandler"
        );

        AtomicInteger received = new AtomicInteger();

        try (
                KafkaConsumer<String,String> consumer =
                        new KafkaConsumer<>(props)
        ) {

            consumer.subscribe(Collections.singletonList(topic));

            Instant deadline =
                    Instant.now().plusSeconds(pollSeconds);

            while (Instant.now().isBefore(deadline)) {

                var records =
                        consumer.poll(Duration.ofSeconds(1));

                for (ConsumerRecord<String,String> r : records) {

                    received.incrementAndGet();

                    log.info(
                            "RECEIVED topic={} partition={} offset={} key={} value={}",
                            r.topic(),
                            r.partition(),
                            r.offset(),
                            r.key(),
                            r.value()
                    );
                }

                if (received.get() > 0)
                    break;
            }
        }

        int exitCode = received.get() > 0 ? 0 : 1;

        log.info(
                "DONE received={} exitCode={}",
                received.get(),
                exitCode
        );

        System.exit(
                org.springframework.boot.SpringApplication.exit(
                        ctx,
                        () -> exitCode
                )
        );
    }
}
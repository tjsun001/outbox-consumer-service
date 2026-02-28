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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.consumer.once.enabled", havingValue = "true")
public class KafkaConsumerOnceRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerOnceRunner.class);

    private final Environment env;
    private final ConfigurableApplicationContext ctx;

    public KafkaConsumerOnceRunner(Environment env, ConfigurableApplicationContext ctx) {
        this.env = env;
        this.ctx = ctx;
    }

    @Override
    public void run(String... args) {
        final String bootstrap = env.getRequiredProperty("app.kafka.bootstrap-servers");
        final String topic = env.getProperty("app.kafka.topic", "outbox.events.test");

        final int pollSeconds = env.getProperty("app.consumer.once.poll-seconds", Integer.class, 30);
        final int maxMessages = env.getProperty("app.consumer.once.max-messages", Integer.class, 1);
        final int pollMs = env.getProperty("app.consumer.once.poll-ms", Integer.class, 1000);
        final int maxPollRecords = env.getProperty("app.consumer.once.max-poll-records", Integer.class, 50);

        final String groupBase = env.getProperty("app.kafka.group-id", "outbox-consumer-smoketest-v1");
        final boolean randomizeGroup = env.getProperty("app.consumer.once.randomize-group", Boolean.class, true);
        final String groupId = randomizeGroup ? groupBase + "-" + UUID.randomUUID() : groupBase;

        final String offsetReset = env.getProperty("app.kafka.auto-offset-reset", "earliest");

        log.info(
                "MODE=consume-once topic={} pollSeconds={} maxMessages={} groupId={} bootstrap={}",
                topic, pollSeconds, maxMessages, groupId, bootstrap
        );

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(maxPollRecords));

        // MSK IAM
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "AWS_MSK_IAM");
        props.put("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;");
        props.put("sasl.client.callback.handler.class", "software.amazon.msk.auth.iam.IAMClientCallbackHandler");

        final AtomicInteger received = new AtomicInteger(0);
        int exitCode = 1;

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));

            final Instant deadline = Instant.now().plusSeconds(pollSeconds);

            while (Instant.now().isBefore(deadline)) {
                var records = consumer.poll(Duration.ofMillis(pollMs));

                for (ConsumerRecord<String, String> r : records) {
                    int n = received.incrementAndGet();
                    log.info("RECEIVED n={} topic={} partition={} offset={} key={} value={}",
                            n, r.topic(), r.partition(), r.offset(), r.key(), r.value());
                    if (n >= maxMessages) {
                        exitCode = 0;
                        break;
                    }
                }

                if (exitCode == 0) break;
            }

            if (received.get() > 0 && exitCode != 0) {
                boolean successIfAny = env.getProperty("app.consumer.once.success-if-any", Boolean.class, true);
                exitCode = successIfAny ? 0 : 1;
            }

        } catch (Exception e) {
            log.error("consume-once FAILED", e);
            exitCode = 2;
        }

        log.info("DONE received={} exitCode={}", received.get(), exitCode);

        int finalExitCode = exitCode;
        int code = SpringApplication.exit(ctx, () -> finalExitCode);

        boolean forceExit = env.getProperty("app.consumer.once.force-exit", Boolean.class, true);
        if (forceExit) System.exit(code);
    }
}
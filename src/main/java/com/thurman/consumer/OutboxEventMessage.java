package com.thurman.consumer;

import java.time.Instant;
import java.util.UUID;

public record OutboxEventMessage(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        Instant createdAt,
        String payloadJson
) {}

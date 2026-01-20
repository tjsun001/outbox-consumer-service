package com.thurman.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository repo;

    public boolean isAlreadyProcessed(java.util.UUID eventId) {
        return repo.existsById(eventId);
    }

    @Transactional
    public void process(OutboxEventMessage msg) {
        // TODO: your real work here:
        // - write to inference_event_log
        // - call inference service
        // - update a projection, etc.

        repo.save(ProcessedEventEntity.processed(msg.id()));
    }
}

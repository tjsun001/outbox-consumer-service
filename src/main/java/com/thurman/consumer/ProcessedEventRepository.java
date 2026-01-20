package com.thurman.consumer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
    // JpaRepository already gives you:
    // existsById(UUID id)
    // save(entity)
    // findById(id)
}

package com.thurman.consumer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {
    @Setter
    @Getter
    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "status", nullable = false)
    private String status;

    public static ProcessedEventEntity processed(UUID id) {
        ProcessedEventEntity e = new ProcessedEventEntity();
        e.eventId = id;
        e.status = "PROCESSED";
        return e;
    }
}

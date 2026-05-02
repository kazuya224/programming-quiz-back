package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_events")
@Getter
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
        this.createdAt = LocalDateTime.now();
    }
}
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "learning_sessions", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "language" }))
public class LearningSession {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String language;

    @Column(name = "current_question_id")
    private UUID currentQuestionId;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // getter / setter（省略せず作る）
}
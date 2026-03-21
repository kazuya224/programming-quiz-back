package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "user_progress") // 設計書の名前に合わせるのが無難
@Data
public class UserProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "progress_id")
    private UUID progressId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "selected_option_id")
    private UUID selectedOptionId; // StringからUUIDへ

    @Column(name = "is_correct")
    private boolean isCorrect;

    @Column(name = "confidence")
    private int confidence;

    @CreationTimestamp
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}
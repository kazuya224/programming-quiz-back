package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "user_progress") // 設計書の名前に合わせるのが無難
@Data
public class UserProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "answer_log_id")
    private UUID answerLogId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "selected_option_id")
    private UUID selectedOptionId; // StringからUUIDへ

    @Column(name = "is_correct")
    @JsonProperty("isCorrect")
    private boolean isCorrect;

    @Column(name = "confidence")
    private int confidence;

    @CreationTimestamp
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @ManyToOne(fetch = FetchType.LAZY) // 1つの問題に対して、複数の回答履歴がある
    @JoinColumn(name = "question_id", insertable = false, updatable = false)
    private Question question;
}
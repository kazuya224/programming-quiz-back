package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "answer_logs")
public class Answerlogs {

    @Id
    @GeneratedValue
    private UUID answerLogId;

    private UUID userId;
    private UUID questionId;

    private Boolean isCorrect;
    private Integer confidence;

    private LocalDateTime answeredAt;
}
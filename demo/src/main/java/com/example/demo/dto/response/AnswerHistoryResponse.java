package com.example.demo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class AnswerHistoryResponse {

    private UUID answerLogId;
    private String title;
    private String genre;
    private String language;
    private boolean isCorrect;
    private int confidence;
    private LocalDateTime answeredAt;

    public AnswerHistoryResponse(
            UUID answerLogId,
            String title,
            String genre,
            String language,
            boolean isCorrect,
            int confidence,
            LocalDateTime answeredAt) {
        this.answerLogId = answerLogId;
        this.title = title;
        this.genre = genre;
        this.language = language;
        this.isCorrect = isCorrect;
        this.confidence = confidence;
        this.answeredAt = answeredAt;
    }
}
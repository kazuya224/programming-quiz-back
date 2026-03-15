package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class HistoryResponse {

    private UUID answerLogId;
    private UUID questionId;
    private String title;
    private String language;
    private Boolean isCorrect;
    private Integer confidence;
    private LocalDateTime answeredAt;

    public HistoryResponse(
            UUID answerLogId,
            UUID questionId,
            String title,
            String language,
            Boolean isCorrect,
            Integer confidence,
            LocalDateTime answeredAt) {
        this.answerLogId = answerLogId;
        this.questionId = questionId;
        this.title = title;
        this.language = language;
        this.isCorrect = isCorrect;
        this.confidence = confidence;
        this.answeredAt = answeredAt;
    }

    public UUID getAnswerLogId() {
        return answerLogId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public String getTitle() {
        return title;
    }

    public String getLanguage() {
        return language;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

}
package com.example.demo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class AnswerHistoryResponse {
    private UUID questionId;
    private UUID selectedOptionId;
    private boolean isCorrect;
    private int confidence;
    private LocalDateTime answeredAt;
}

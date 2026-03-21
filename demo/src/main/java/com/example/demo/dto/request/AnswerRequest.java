package com.example.demo.dto.request;

import java.util.UUID;

import lombok.Data;

@Data
public class AnswerRequest {
    private UUID userId;
    private UUID questionId;
    private UUID selectedOptionId;
    private boolean isCorrect;
    private int confidence;
}

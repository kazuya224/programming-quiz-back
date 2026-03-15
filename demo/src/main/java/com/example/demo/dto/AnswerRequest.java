package com.example.demo.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class AnswerRequest {
    private UUID userId;
    private UUID questionId;
    private Boolean isCorrect;
    private Integer confidence;
}

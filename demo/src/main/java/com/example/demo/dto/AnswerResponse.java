package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class AnswerResponse {
    private UUID answerLogId;
    private UUID userId;
    private UUID questoinId;
    private Boolean isCorrect;
    private Integer confidence;
    private LocalDateTime answerAt;
}

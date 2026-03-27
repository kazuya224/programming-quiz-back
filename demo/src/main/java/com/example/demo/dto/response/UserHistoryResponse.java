package com.example.demo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@AllArgsConstructor
public class UserHistoryResponse {
    private UUID questionId;
    private String questionText;
    private String genre;
    private String language;
    private boolean isCorrect;
    private int confidence;
    private LocalDateTime answeredAt;
}

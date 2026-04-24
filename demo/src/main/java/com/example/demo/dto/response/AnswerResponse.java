package com.example.demo.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnswerResponse {
    private boolean correct;
    private UUID correctOptionId;
    private String explanation;
}

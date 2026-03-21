package com.example.demo.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class QuestionDto {
    private UUID questionId;
    private String questionText;
    private String codeSnippet;
    private List<OptionDto> options;
    private String title;
    private String language;
    private String genre;
    private int difficultyLevel;
    private String explanation;
}
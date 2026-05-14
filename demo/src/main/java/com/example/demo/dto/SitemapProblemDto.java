package com.example.demo.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SitemapProblemDto {

    private UUID questionId;
    private String language;
    private String genre;
    private int difficultyLevel;
}
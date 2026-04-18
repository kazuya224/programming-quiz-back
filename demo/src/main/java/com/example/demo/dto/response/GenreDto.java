package com.example.demo.dto.response;

public record GenreDto(
                String genre,
                String language,
                Double accuracy, // 未回答はnull
                long totalCount,
                long correctCount) {
}
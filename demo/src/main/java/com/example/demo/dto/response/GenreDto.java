package com.example.demo.dto.response;

public record GenreDto(
        String genre,
        Double accuracy, // 未回答はnull
        long totalCount,
        long correctCount) {
}
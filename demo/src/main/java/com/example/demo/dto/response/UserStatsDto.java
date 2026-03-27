package com.example.demo.dto.response;

public record UserStatsDto(
        long totalAnswers, // 総解答数
        long correctAnswers, // 正解数
        int accuracyRate, // 正解率（%）
        long masteredCount // 習得済み（例：正解したユニークな問題数）
) {
}
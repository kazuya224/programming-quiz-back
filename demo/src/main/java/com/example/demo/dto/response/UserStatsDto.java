package com.example.demo.dto.response;

public record UserStatsDto(
                // 今日
                int todayCount,

                // 継続
                int streak,

                // 今週
                WeekStats thisWeek,

                // 前週
                WeekStats lastWeek,

                // 変化
                DiffStats diff,

                // 再開
                boolean hasResume) {
}
package com.example.demo.dto.response;

import java.util.List;
import java.util.Map;
import com.example.demo.dto.response.UserStatsDto;

public record DashboardResponse(UserStatsDto stats,
        Map<String, List<GenreDto>> genres) {
}
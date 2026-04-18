package com.example.demo.controller;

import com.example.demo.dto.response.DashboardResponse;
import com.example.demo.dto.response.GenreDto;
import com.example.demo.dto.response.QuestionResponse;
import com.example.demo.dto.response.UserStatsDto;
import com.example.demo.entity.Question;
import com.example.demo.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    // 1. ページングされた問題取得
    @GetMapping
    public ResponseEntity<QuestionResponse> getQuestions(
            @RequestParam String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(questionService.getQuestions(language, page, size));
    }

    // 2. 間違えた問題だけ取得
    @GetMapping("/mistakes")
    public ResponseEntity<QuestionResponse> getIncorrectQuestions(@RequestParam UUID userId,
            @RequestParam(required = false) String genre,
            @RequestParam String language, @RequestParam int page, @RequestParam int size) {
        return ResponseEntity.ok(questionService.getIncorrectQuestions(userId, genre, language, page, size));
    }

    // 3. 途中から再開
    @GetMapping("/resume")
    public ResponseEntity<QuestionResponse> resume(@RequestParam UUID userId,
            @RequestParam(required = false) String genre, @RequestParam String language, @RequestParam int page,
            @RequestParam int limit) {
        return ResponseEntity.ok(questionService.resumeQuestions(userId, genre, language, page, limit));
    }

    // 4. 統計データ取得
    @GetMapping("/stats/{userId}")
    public DashboardResponse getUserStats(@PathVariable UUID userId) {
        UserStatsDto stats = questionService.getUserStats(userId);
        Map<String, List<GenreDto>> genres = questionService.getGenreStats(userId);
        return new DashboardResponse(stats, genres);
    }

}
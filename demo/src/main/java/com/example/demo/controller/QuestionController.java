package com.example.demo.controller;

import com.example.demo.dto.response.QuestionResponse;
import com.example.demo.dto.response.UserStatsDto;
import com.example.demo.entity.Question;
import com.example.demo.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<Question>> getMistakes(@RequestParam UUID userId) {
        return ResponseEntity.ok(questionService.getMistakenQuestions(userId));
    }

    // 3. 途中から再開
    @GetMapping("/resume")
    public ResponseEntity<QuestionResponse> resume(@RequestParam UUID userId, @RequestParam int limit) {
        return ResponseEntity.ok(questionService.resumeQuestions(userId, limit));
    }

    // 4. 統計データ取得
    @GetMapping("/stats/{userId}")
    public ResponseEntity<UserStatsDto> getUserStats(@PathVariable UUID userId) {
        return ResponseEntity.ok(questionService.getuserStats(userId));
    }

}
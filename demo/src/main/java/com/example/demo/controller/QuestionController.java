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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    // 1. 通常問題（cursor方式）
    @GetMapping
    public ResponseEntity<QuestionResponse> getQuestions(
            @RequestParam String language,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                questionService.getQuestions(language, cursor, size));
    }

    // 2. 間違えた問題
    @GetMapping("/mistakes")
    public ResponseEntity<QuestionResponse> getIncorrectQuestions(
            Authentication authentication,
            @RequestParam(required = false) String genre,
            @RequestParam String language,
            @RequestParam(required = false) Long cursor,
            @RequestParam int size) {

        // ❌ NG: UUID userId = (UUID) authentication.getPrincipal();

        // ✅ 正解
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                questionService.getIncorrectQuestions(userId, genre, language, cursor, size));
    }

    // 3. 再開
    @GetMapping("/resume")
    public ResponseEntity<QuestionResponse> resume(
            Authentication authentication,
            @RequestParam(required = false) String genre,
            @RequestParam String language,
            @RequestParam(required = false) Long cursor,
            @RequestParam int size) {

        // ★ 修正ポイント
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // subからuserId取り出す（←これが本質）
        UUID userId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                questionService.resumeQuestions(userId, genre, language, cursor, size));
    }

    // 4. 統計データ取得
    @GetMapping("/stats")
    public DashboardResponse getUserStats(@AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        UserStatsDto stats = questionService.getUserStats(userId);
        Map<String, List<GenreDto>> genres = questionService.getGenreStats(userId);

        return new DashboardResponse(stats, genres);
    }

}
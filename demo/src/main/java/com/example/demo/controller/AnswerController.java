package com.example.demo.controller;

import com.example.demo.dto.request.AnswerRequest; // リクエストボディ用のDTO
import com.example.demo.dto.response.AnswerHistoryResponse;
import com.example.demo.dto.response.UserHistoryResponse;
import com.example.demo.entity.UserProgress;
import com.example.demo.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    // 1. 解答送信
    @PostMapping
    public ResponseEntity<Map<String, String>> submitAnswer(@RequestBody AnswerRequest request) {
        answerService.saveAnswer(
                request.getUserId(),
                request.getQuestionId(),
                request.getSelectedOptionId(),
                request.getConfidence());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // 2. 解答履歴取得
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<AnswerHistoryResponse>> getHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(answerService.getHistoryByUserId(userId));
    }
}
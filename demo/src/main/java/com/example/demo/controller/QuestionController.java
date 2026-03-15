package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import com.example.demo.service.QuestionService;
import com.example.demo.service.AnswerlogsService;
import com.example.demo.entity.Question;
import com.example.demo.entity.Answerlogs;
import com.example.demo.dto.AnswerRequest;
import com.example.demo.dto.HistoryResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*") // フロントエンドからのアクセスを許可
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerlogsService answerlogsService;

    // 1. 問題取得（全件・フィルタ共通化）
    // GET /api/questions または /api/questions?language=Java
    @GetMapping
    public List<Question> getQuestions(@RequestParam(required = false) String language) {
        if (language != null && !language.isEmpty()) {
            return questionService.getQuestionsByLanguage(language);
        }
        return questionService.getAllQuestions();
    }

    // 2. 解答履歴の保存
    // POST /api/questions/history
    @PostMapping("/history")
    public ResponseEntity<Answerlogs> recordAnswer(@RequestBody AnswerRequest request) {
        Answerlogs saved = answerlogsService.saveProgress(request);
        return ResponseEntity.ok(saved);
    }

    // 3. ユーザーごとの履歴一覧取得
    // GET /api/questions/history/{userId}
    @GetMapping("/history/{userId}")
    public List<HistoryResponse> getUserHistory(@PathVariable UUID userId) {
        System.out.println("レスポンス" + answerlogsService.getHistoryByUserId(userId));
        return answerlogsService.getHistoryByUserId(userId);
    }

    // 4. 学習統計データの取得
    // GET /api/questions/stats/{userId}
    @GetMapping("/stats/{userId}")
    public Map<String, Object> getUserStats(@PathVariable UUID userId) {
        // Service側で集計した「正解率」「習得数」などのMapを返す
        return answerlogsService.calculateStats(userId);
    }
}
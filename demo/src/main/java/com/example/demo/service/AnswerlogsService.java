package com.example.demo.service;

import com.example.demo.dto.AnswerRequest;
import com.example.demo.dto.HistoryRequest;
import com.example.demo.dto.HistoryResponse;
import com.example.demo.entity.Question;
import com.example.demo.entity.Answerlogs;
import com.example.demo.entity.UserProgressId;
import com.example.demo.repository.AnswerlogsRepository;
import com.example.demo.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerlogsService {

    private final QuestionRepository questionRepository;
    private final AnswerlogsRepository answerlogsRepository;

    // 1. 回答保存（履歴なので毎回INSERT）
    @Transactional
    public Answerlogs saveProgress(AnswerRequest request) {

        Answerlogs log = new Answerlogs();

        log.setUserId(request.getUserId());
        log.setQuestionId(request.getQuestionId());
        log.setIsCorrect(request.getIsCorrect());
        log.setConfidence(request.getConfidence());
        log.setAnsweredAt(LocalDateTime.now());

        return answerlogsRepository.save(log);
    }

    // 2. 履歴取得
    public List<HistoryResponse> getHistoryByUserId(UUID userId) {
        return answerlogsRepository.findHistoryByUserId(userId);
    }

    // 3. 統計
    public Map<String, Object> calculateStats(UUID userId) {

        List<Answerlogs> logs = answerlogsRepository.findByUserIdOrderByAnsweredAtDesc(userId);

        long totalAnswers = logs.size();
        long correctAnswers = logs.stream()
                .filter(Answerlogs::getIsCorrect)
                .count();

        long confidentCorrect = logs.stream()
                .filter(l -> l.getIsCorrect() && l.getConfidence() == 1)
                .count();

        double accuracyRate = totalAnswers > 0 ? (double) correctAnswers / totalAnswers * 100 : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAnswers", totalAnswers);
        stats.put("correctAnswers", correctAnswers);
        stats.put("accuracyRate", Math.round(accuracyRate));
        stats.put("masteredCount", confidentCorrect);

        return stats;
    }
}
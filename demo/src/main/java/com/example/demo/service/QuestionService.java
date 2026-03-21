package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.demo.repository.OptionRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserProgressRepository;

import lombok.RequiredArgsConstructor;

import com.example.demo.dto.response.QuestionResponse;
import com.example.demo.entity.Question;
import com.example.demo.entity.UserProgress;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final UserProgressRepository userProgressRepository;

    // 1. ページングされた問題取得 (/api/questions)
    public Page<Question> getQuestions(String language, int page, int size) {
        return questionRepository.findByLanguage(language, PageRequest.of(page, size));
    }

    // 2. 解答履歴取得 (/api/answers/history/{userId})
    public List<UserProgress> getHistory(UUID userId) {
        return userProgressRepository.findByUserIdOrderByAnsweredAtDesc(userId);
    }

    // 3. 間違えた問題だけ取得 (/api/questions/mistakes)
    public List<Question> getMistakenQuestions(UUID userId) {
        List<UUID> ids = userProgressRepository.findDistinctQuestionIdsByUserIdAndIsCorrectFalse(userId);
        return questionRepository.findAllById(ids);
    }

    // 4. 途中から再開 (/api/questions/resume)
    // 最後に解いた問題の「次」から取得するロジックの雛形
    public Page<Question> resumeQuestions(UUID userId, int limit) {
        return userProgressRepository.findFirstByUserIdOrderByAnsweredAtDesc(userId)
                .map(last -> {
                    // 本来は「最後に解いたID以降」などの条件が必要ですが、
                    // まずはシンプルに最新の続きを取得する仕組みにします
                    return questionRepository.findAll(PageRequest.of(0, limit));
                })
                .orElse(questionRepository.findAll(PageRequest.of(0, limit)));
    }
}
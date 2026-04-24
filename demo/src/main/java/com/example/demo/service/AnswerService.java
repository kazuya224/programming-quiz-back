package com.example.demo.service;

import com.example.demo.entity.UserProgress;
import com.example.demo.dto.request.AnswerRequest;
import com.example.demo.dto.response.AnswerHistoryResponse;
import com.example.demo.dto.response.AnswerResponse;
import com.example.demo.dto.response.UserHistoryResponse;
import com.example.demo.entity.Option;
import com.example.demo.entity.Question;
import com.example.demo.repository.OptionRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnswerService {

        private final OptionRepository optionRepository;
        // 変数名を統一（設計書に合わせて userProgressRepository とします）
        private final UserProgressRepository userProgressRepository;
        private final QuestionRepository questionRepository;

        // 1. 回答保存（API: /api/answers）
        @Transactional
        public AnswerResponse submitAnswer(UUID userId, AnswerRequest request) {

                // 正解の選択肢取得
                Option correctOption = optionRepository
                                .findByQuestionIdAndIsCorrectTrue(request.getQuestionId())
                                .orElseThrow();

                boolean isCorrect = correctOption.getOptionId()
                                .equals(request.getSelectedOptionId());

                // 解説取得
                Question question = questionRepository
                                .findById(request.getQuestionId())
                                .orElseThrow();

                // 保存
                UserProgress progress = new UserProgress();
                progress.setUserId(userId);
                progress.setQuestionId(request.getQuestionId());
                progress.setSelectedOptionId(request.getSelectedOptionId());
                progress.setCorrect(isCorrect);
                progress.setConfidence(request.getConfidence());
                progress.setAnsweredAt(LocalDateTime.now());

                userProgressRepository.save(progress);

                // 👇 フロントに返す
                return new AnswerResponse(
                                isCorrect,
                                correctOption.getOptionId(),
                                question.getExplanation());
        }

        // 2. 履歴取得（API: /api/answers/history/{userId}）
        public List<AnswerHistoryResponse> getHistoryByUserId(UUID userId) {
                List<UserProgress> history = userProgressRepository
                                .findHistoryWithQuestion(userId);

                return history.stream()
                                .map(progress -> {
                                        Question q = progress.getQuestion();

                                        return new AnswerHistoryResponse(
                                                        progress.getAnswer_log_id(),
                                                        q.getTitle(),
                                                        q.getGenre(),
                                                        q.getLanguage(),
                                                        progress.isCorrect(),
                                                        progress.getConfidence(),
                                                        progress.getAnsweredAt());
                                })
                                .toList();
        }
}
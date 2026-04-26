package com.example.demo.service;

import com.example.demo.entity.UserProgress;
import com.example.demo.dto.request.AnswerRequest;
import com.example.demo.dto.response.AnswerHistoryResponse;
import com.example.demo.dto.response.AnswerResponse;
import com.example.demo.dto.response.UserHistoryResponse;
import com.example.demo.entity.LearningSession;
import com.example.demo.entity.Option;
import com.example.demo.entity.Question;
import com.example.demo.repository.LearningSessionRepository;
import com.example.demo.repository.OptionRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.jwt.Jwt;
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

        @Autowired
        private LearningSessionRepository learningSessionRepository;

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

                if ("learning".equals(request.getMode()) || "resume".equals(request.getMode())) {

                        LearningSession session = learningSessionRepository
                                        .findByUserIdAndLanguage(userId, request.getLanguage())
                                        .orElseGet(() -> {
                                                LearningSession newSession = new LearningSession();
                                                newSession.setUserId(userId);
                                                newSession.setLanguage(request.getLanguage());
                                                return newSession;
                                        });

                        session.setCurrentQuestionId(request.getQuestionId());
                        learningSessionRepository.save(session);
                }

                // 👇 フロントに返す
                return new AnswerResponse(
                                isCorrect,
                                correctOption.getOptionId(),
                                question.getExplanation());
        }

        public UUID getNextQuestionId(UUID currentQuestionId, String language) {

                Long currentSeq = questionRepository
                                .findSeqByQuestionId(currentQuestionId)
                                .orElseThrow();

                return questionRepository
                                .findNextQuestions(language, currentSeq, PageRequest.of(0, 1))
                                .stream()
                                .findFirst()
                                .map(Question::getQuestionId)
                                .orElse(null);
        }

        // 2. 履歴取得（API: /api/answers/history/{userId}）
        public List<AnswerHistoryResponse> getHistoryByUserId(UUID userId) {
                List<UserProgress> history = userProgressRepository
                                .findHistoryWithQuestion(userId);

                return history.stream()
                                .map(progress -> {
                                        Question q = progress.getQuestion();

                                        return new AnswerHistoryResponse(
                                                        progress.getAnswerLogId(),
                                                        q.getTitle(),
                                                        q.getGenre(),
                                                        q.getLanguage(),
                                                        progress.isCorrect(),
                                                        progress.getConfidence(),
                                                        progress.getAnsweredAt());
                                })
                                .toList();
        }

        public Page<AnswerHistoryResponse> getHistory(UUID userId, int page, int size) {
                Pageable pageable = PageRequest.of(page, size);
                return userProgressRepository.findHistoryDto(userId, pageable);
        }
}
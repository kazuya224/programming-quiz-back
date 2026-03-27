package com.example.demo.service;

import com.example.demo.entity.UserProgress;
import com.example.demo.dto.response.AnswerHistoryResponse;
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
        public void saveAnswer(UUID userId, UUID questionId, UUID selectedOptionId, int confidence) {
                System.out.println("リクエスト" + selectedOptionId.toString());
                // サーバーサイドでの正誤判定（不正防止の仕組み）
                boolean isCorrect = optionRepository.findById(selectedOptionId)
                                .map(Option::isCorrect)
                                .orElse(false);

                UserProgress progress = new UserProgress();
                progress.setUserId(userId);
                progress.setQuestionId(questionId);
                progress.setSelectedOptionId(selectedOptionId);
                progress.setCorrect(isCorrect);
                progress.setConfidence(confidence);
                progress.setAnsweredAt(LocalDateTime.now()); // 明示的にセット

                userProgressRepository.save(progress);
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

        // 3. 統計（ダッシュボード等で活用できる仕組み）
        public Map<String, Object> calculateStats(UUID userId) {
                List<UserProgress> logs = userProgressRepository.findByUserIdOrderByAnsweredAtDesc(userId);

                long totalAnswers = logs.size();
                // boolean型のゲッター名はLombokの設定により isCorrect() か getIsCorrect() になります
                long correctAnswers = logs.stream()
                                .filter(UserProgress::isCorrect)
                                .count();

                // 「正解」かつ「自信あり（例：confidence=2）」の数を集計
                long confidentCorrect = logs.stream()
                                .filter(l -> l.isCorrect() && l.getConfidence() == 2)
                                .count();

                double accuracyRate = totalAnswers > 0 ? (double) correctAnswers / totalAnswers * 100 : 0;

                Map<String, Object> stats = new HashMap<>();
                stats.put("totalAnswers", totalAnswers);
                stats.put("correctAnswers", correctAnswers);
                stats.put("accuracyRate", Math.round(accuracyRate));
                stats.put("masteredCount", confidentCorrect); // 自信を持って正解した数

                return stats;
        }
}
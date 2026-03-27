package com.example.demo.repository;

import com.example.demo.entity.UserProgress;
import com.example.demo.dto.response.AnswerHistoryResponse;
import com.example.demo.dto.response.UserHistoryResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProgressRepository extends JpaRepository<UserProgress, UUID> {

    // 1. 解答履歴取得用 (/api/answers/history/{userId})
    List<UserProgress> findByUserIdOrderByAnsweredAtDesc(UUID userId);

    // 2. 途中から再開用 (最後に解いた1件を特定)
    Optional<UserProgress> findFirstByUserIdOrderByAnsweredAtDesc(UUID userId);

    // 3. 間違えた問題IDの抽出用 (/api/questions/mistakes)
    @Query("SELECT DISTINCT up.questionId FROM UserProgress up WHERE up.userId = :userId AND up.isCorrect = false")
    List<UUID> findDistinctQuestionIdsByUserIdAndIsCorrectFalse(@Param("userId") UUID userId);

    // 4. ユーザーごとに総解答数取得
    long countByUserId(UUID userId);

    // 正解数を取得
    long countByUserIdAndIsCorrectTrue(UUID userId);

    // 習得済み数
    // UserProgressRepository.java

    @Query("SELECT COUNT(DISTINCT up.questionId) FROM UserProgress up WHERE up.userId = :userId AND up.isCorrect = true")
    long countDistinctQuestionIdByUserIdAndIsCorrectTrue(@Param("userId") UUID userId);

    // 解答と問題文、言語を取得（履歴画面）
    @Query("""
            SELECT up FROM UserProgress up JOIN FETCH up.question WHERE up.userId = :userId ORDER BY up.answeredAt DESC
            """)
    List<UserProgress> findHistoryWithQuestion(UUID userId);

}
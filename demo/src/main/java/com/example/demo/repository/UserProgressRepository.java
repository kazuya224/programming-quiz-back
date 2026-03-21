package com.example.demo.repository;

import com.example.demo.entity.UserProgress;
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
}
package com.example.demo.repository;

import com.example.demo.entity.UserProgress;
import com.example.demo.repository.projection.GenreStatsProjection;
import com.example.demo.dto.response.AnswerHistoryResponse;
import com.example.demo.dto.response.UserHistoryResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        // 4. ユーザーごとに今日の解答数取得
        int countByUserIdAndAnsweredAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

        // 1週間の解答数を取得
        @Query("""
                        SELECT COUNT(up) FROM UserProgress up WHERE up.userId =:userId AND up.answeredAt >= :start AND up.answeredAt <:end
                        """)
        long countByPeriod(UUID userId, LocalDateTime start, LocalDateTime end);

        // 1週間の正答数を取得
        @Query("""
                        SELECT COUNT(up) FROM UserProgress up WHERE up.userId =:userId AND up.isCorrect = true AND up.answeredAt >= :start AND up.answeredAt <:end
                        """)
        long countCorrectByPeriod(UUID userId, LocalDateTime start, LocalDateTime end);

        // 継続日数
        @Query(value = """
                                    SELECT DISTINCT (answered_at AT TIME ZONE 'Asia/Tokyo')::date
                        FROM user_progress
                        WHERE user_id = :userId
                        ORDER BY 1 DESC
                                    """, nativeQuery = true)
        List<LocalDate> findAnsweredDatesJST(UUID userId);

        // 過去に問題を解いたことがあるか判定
        long countByUserId(UUID userId);

        // ダッシュボードで言語、ジャンルを表示
        @Query("""
                                       SELECT
                        q.language as language,
                        q.genre as genre,
                        COUNT(up) as totalCount,
                        SUM(CASE WHEN up.isCorrect = true THEN 1 ELSE 0 END) as correctCount
                            FROM UserProgress up
                            JOIN up.question q
                            WHERE up.userId =:userId
                            GROUP BY q.language, q.genre
                                        """)
        List<GenreStatsProjection> getGenreStats(UUID userId);

        // 解答と問題文、言語を取得（履歴画面）
        @Query("""
                        SELECT up FROM UserProgress up JOIN FETCH up.question WHERE up.userId = :userId ORDER BY up.answeredAt DESC
                        """)
        List<UserProgress> findHistoryWithQuestion(UUID userId);

}
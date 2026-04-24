package com.example.demo.repository;

import com.example.demo.entity.UserProgress;
import com.example.demo.entity.Question;
import com.example.demo.repository.projection.GenreStatsProjection;
import com.example.demo.dto.response.AnswerHistoryResponse;
import com.example.demo.dto.response.GenreDto;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserProgressRepository extends JpaRepository<UserProgress, UUID> {

    // 1. 解答履歴取得用 (/api/answers/history/{userId})
    List<UserProgress> findByUserIdOrderByAnsweredAtDesc(UUID userId);

    // 2. 途中から再開用 (最後に解いた1件を特定)
    Optional<UserProgress> findFirstByUserIdOrderByAnsweredAtDesc(UUID userId);

    Optional<UserProgress> findFirstByUserIdAndQuestionLanguageOrderByAnsweredAtDesc(UUID userId, String language);

    // 3. 間違えた問題IDの抽出用 (/api/questions/mistakes)
    @Query("SELECT up.question FROM UserProgress up " +
            "WHERE up.userId = :userId AND up.isCorrect = false " +
            "AND (:genre IS NULL OR up.question.genre = :genre) " +
            "AND (:language IS NULL OR up.question.language = :language) " +
            "GROUP BY up.question.questionId, up.question " + // 重複を排除
            "ORDER BY MAX(up.answeredAt) DESC") // 各問題の最新ミス日時で並び替え
    Page<Question> findIncorrectQuestionsByUserId(
            @Param("userId") UUID userId,
            @Param("genre") String genre,
            @Param("language") String language,
            Pageable pageable);

    @Query("SELECT up FROM UserProgress up JOIN up.question q " +
            "WHERE up.userId = :userId AND q.language = :language " +
            "AND (:genre IS NULL OR q.genre = :genre) " +
            "ORDER BY up.answeredAt DESC LIMIT 1")
    Optional<UserProgress> findFirstByUserIdAndQuestionLanguageAndQuestionGenreOrderByAnsweredAtDesc(
            @Param("userId") UUID userId,
            @Param("language") String language,
            @Param("genre") String genre);

    @Query(value = """
                SELECT q.*
                FROM questions q
                WHERE EXISTS (
                    SELECT 1
                    FROM user_progress up
                    WHERE up.user_id = :userId
                    AND up.question_id = q.question_id
                    AND up.answered_at = (
                        SELECT MAX(up2.answered_at)
                        FROM user_progress up2
                        WHERE up2.user_id = :userId
                        AND up2.question_id = q.question_id
                    )
                    AND up.is_correct = false
                )
                AND q.language = :language
                AND (:genre IS NULL OR q.genre = :genre)
                AND (:cursor IS NULL OR q.seq > :cursor)
                ORDER BY q.seq ASC
            """, nativeQuery = true)
    List<Question> findIncorrectQuestionsWithCursor(
            @Param("userId") UUID userId,
            @Param("genre") String genre,
            @Param("language") String language,
            @Param("cursor") Long cursor,
            Pageable pageable);

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
    @Query(value = """
                SELECT
                    q.language as language,
                    q.genre as genre,
                    COUNT(*) as totalCount,
                    SUM(CASE WHEN up.is_correct THEN 1 ELSE 0 END) as correctCount
                FROM (
                    SELECT DISTINCT ON (question_id)
                        question_id,
                        is_correct
                    FROM user_progress
                    WHERE user_id = :userId
                    ORDER BY question_id, answered_at DESC
                ) up
                JOIN questions q ON q.question_id = up.question_id
                GROUP BY q.language, q.genre
            """, nativeQuery = true)
    List<GenreStatsProjection> getGenreStats(UUID userId);

    @Query("SELECT q.language as language, q.genre as genre, COUNT(q) as totalCount " + // ← ここ！
            " FROM Question q " + // ← ここ！
            " GROUP BY q.language, q.genre")
    List<GenreDto> findAllQuestionCounts();

    // 解答と問題文、言語を取得（履歴画面）
    @Query("""
            SELECT up FROM UserProgress up JOIN FETCH up.question WHERE up.userId = :userId ORDER BY up.answeredAt DESC
            """)
    List<UserProgress> findHistoryWithQuestion(UUID userId);

}
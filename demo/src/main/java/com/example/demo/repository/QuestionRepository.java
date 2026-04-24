package com.example.demo.repository;

import com.example.demo.entity.Option;
import com.example.demo.entity.Question;
import com.example.demo.dto.response.GenreDto;
import com.example.demo.repository.projection.GenreMasterProjection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // ← ここが重要
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

        @Query("SELECT new com.example.demo.dto.response.GenreDto(q.genre, q.language, null, COUNT(q), 0L) " + // ← ここ！
                        " FROM Question q " + // ← ここ！
                        " WHERE q.difficultyLevel = 0" + // ← ここ！
                        " GROUP BY q.language, q.genre")
        List<GenreDto> findAllTotalCounts();

        @Query("SELECT DISTINCT q.language as language, q.genre as genre FROM Question q")
        List<GenreMasterProjection> findAllLanguagesAndGenres();

        @Query("SELECT q FROM Question q " +
                        "WHERE q.language = :language " +
                        "AND (:genre IS NULL OR q.genre = :genre) " +
                        "AND q.seq > :seq ORDER BY q.seq ASC")
        Page<Question> findByLanguageAndGenreAndSeqGreaterThanOrderBySeqAsc(
                        @Param("language") String language,
                        @Param("genre") String genre,
                        @Param("seq") Long seq,
                        Pageable pageable);

        @Query("""
                        SELECT q FROM Question q
                        WHERE q.language = :language
                        AND (:genre IS NULL OR q.genre = :genre)
                        AND (:cursor IS NULL OR q.seq > :cursor)
                        ORDER BY q.seq ASC
                        """)
        List<Question> findQuestionsWithCursor(
                        @Param("language") String language,
                        @Param("genre") String genre,
                        @Param("cursor") Long cursor,
                        Pageable pageable);

        List<Question> findByLanguageOrderBySeqAsc(
                        String language,
                        Pageable pageable);

        List<Question> findByLanguageAndSeqGreaterThanOrderBySeqAsc(
                        String language,
                        Long seq,
                        Pageable pageable);

}
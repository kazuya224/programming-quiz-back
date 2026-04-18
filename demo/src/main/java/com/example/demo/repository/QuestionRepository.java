package com.example.demo.repository;

import com.example.demo.entity.Question;
import com.example.demo.dto.response.GenreDto;
import com.example.demo.repository.projection.GenreMasterProjection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // ← ここが重要
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
        // ページング対応の問題取得
        Page<Question> findByLanguage(String language, Pageable pageable);

        Page<Question> findBySeqGreaterThanOrderBySeqAsc(Integer seq, Pageable pageable);

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
                        @Param("seq") Integer seq,
                        Pageable pageable);
}
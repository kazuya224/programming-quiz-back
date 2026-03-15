package com.example.demo.repository;

import com.example.demo.dto.HistoryResponse;
import com.example.demo.entity.Answerlogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.List;

public interface AnswerlogsRepository extends JpaRepository<Answerlogs, UUID> {

    List<Answerlogs> findByUserIdOrderByAnsweredAtDesc(UUID userId);

    @Query("""
                SELECT new com.example.demo.dto.HistoryResponse(
                    al.answerLogId,
                    q.id,
                    q.title,
                    q.language,
                    al.isCorrect,
                    al.confidence,
                    al.answeredAt
                )
                FROM Answerlogs al
                JOIN Question q ON al.questionId = q.Id
                WHERE al.userId = :userId
                ORDER BY al.answeredAt DESC
            """)
    List<HistoryResponse> findHistoryByUserId(@Param("userId") UUID userId);
}
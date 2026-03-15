package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.entity.Question;
import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByLanguage(String language);

    @Query("""
            SELECT q
            FROM Question q
            LEFT JOIN FETCH q.options
            """)
    List<Question> findAllWithOptions();
}

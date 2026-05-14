package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.dto.SitemapProblemDto;
import com.example.demo.entity.Question;

@Repository
public interface ProblemRepository
        extends JpaRepository<Question, UUID> {

    @Query("""
                SELECT new com.example.demo.dto.SitemapProblemDto(
                    q.questionId,
                    q.language,
                    q.genre,
                    q.difficultyLevel
                )
                FROM Question q
            """)
    List<SitemapProblemDto> findForSitemap();
}
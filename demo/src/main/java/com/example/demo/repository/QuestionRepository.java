package com.example.demo.repository;

import com.example.demo.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // ← ここが重要
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
        // ページング対応の問題取得
        Page<Question> findByLanguage(String language, Pageable pageable);

        Page<Question> findBySeqGreaterThanOrderBySeqAsc(Integer seq, Pageable pageable);
}
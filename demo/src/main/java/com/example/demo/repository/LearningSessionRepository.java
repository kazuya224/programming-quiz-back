package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.LearningSession;

import java.util.Optional;
import java.util.UUID;

public interface LearningSessionRepository
        extends JpaRepository<LearningSession, UUID> {

    Optional<LearningSession> findByUserIdAndLanguage(UUID userId, String language);
}

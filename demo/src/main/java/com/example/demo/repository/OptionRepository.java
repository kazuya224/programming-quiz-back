package com.example.demo.repository;

import com.example.demo.entity.Option;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionRepository extends JpaRepository<Option, UUID> {
    List<Option> findByQuestionId(UUID questionId);

    List<Option> findByQuestionIdOrderByOptionOrderAsc(UUID questionId);

    List<Option> findByQuestionIdIn(List<UUID> questionIds);

}
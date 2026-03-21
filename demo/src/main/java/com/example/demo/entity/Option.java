package com.example.demo.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "options")
@Data
public class Option {

    @Id
    @Column(name = "option_id")
    private UUID optionId;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "option_text")
    private String optionText;

    @Column(name = "option_order")
    private int optionOrder;

    @Column(name = "is_correct")
    private boolean isCorrect;
}
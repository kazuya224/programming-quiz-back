package com.example.demo.entity;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "options")
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "option_id")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "question_id")
    @JsonIgnore
    private Question question;

    @Column(name = "option_text")
    private String optionText;

    @Column(name = "option_order")
    private Integer optionOrder;

    @Column(name = "is_correct")
    private Boolean isCorrect;
}
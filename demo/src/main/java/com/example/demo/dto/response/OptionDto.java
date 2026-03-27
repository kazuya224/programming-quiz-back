package com.example.demo.dto.response;

import java.util.UUID;

import lombok.Data;

@Data
public class OptionDto {
    private UUID optionId;
    private String optionText;
    private int optionOrder;
    private boolean isCorrect;
}

package com.example.demo.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class QuestionResponse {

    private List<QuestionDto> questions;
    private Integer nextCursor; // mistakesはnullになる
    private boolean hasMore;
}
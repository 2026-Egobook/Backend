package com.example.egobook_be.domain.question.dto;

import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MyTodayAnswerResDto(
        Long answerId,
        String content,
        AnswerVisibility visibility,
        LocalDateTime answeredAt
) {}

package com.example.egobook_be.domain.question.dto;

import com.example.egobook_be.domain.question.enums.AnswerVisibility;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record MyAnswerHistoryResDto(
        Long questionId,
        LocalDate questionDate,
        String questionContent,

        Long answerId,
        String answerContent,
        AnswerVisibility visibility,
        LocalDateTime answeredAt
) {
}
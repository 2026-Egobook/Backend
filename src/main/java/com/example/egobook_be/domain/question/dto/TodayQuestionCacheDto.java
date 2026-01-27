package com.example.egobook_be.domain.question.dto;

import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDate;

@Builder
public record TodayQuestionCacheDto(
        Long questionId,
        String content,
        LocalDate date
) implements Serializable {
}

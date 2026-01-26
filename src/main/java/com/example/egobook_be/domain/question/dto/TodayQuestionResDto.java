package com.example.egobook_be.domain.question.dto;

import lombok.Builder;

import java.time.LocalDate;


@Builder
public record TodayQuestionResDto(
        Long questionId,
        String content,
        LocalDate date,
        boolean answered,
        MyTodayAnswerResDto myAnswer
) {}

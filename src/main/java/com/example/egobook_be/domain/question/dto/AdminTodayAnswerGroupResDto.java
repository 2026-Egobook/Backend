package com.example.egobook_be.domain.question.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminTodayAnswerGroupResDto(
        Long questionId,
        LocalDate questionDate,
        String questionContent,
        List<AdminTodayAnswerItemResDto> answers
) {}

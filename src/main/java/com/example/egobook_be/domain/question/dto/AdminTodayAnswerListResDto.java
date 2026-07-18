package com.example.egobook_be.domain.question.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminTodayAnswerListResDto(
        LocalDate startDate,
        LocalDate endDate,
        List<AdminTodayAnswerGroupResDto> questions
) {}

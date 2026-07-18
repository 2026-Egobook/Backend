package com.example.egobook_be.domain.question.dto;

import java.time.LocalDateTime;

public record AdminTodayAnswerItemResDto(
        Long answerId,
        String content,
        LocalDateTime createdAt,
        String accountCode
) {}

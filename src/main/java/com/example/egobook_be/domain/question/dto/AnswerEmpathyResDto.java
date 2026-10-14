package com.example.egobook_be.domain.question.dto;

import lombok.Builder;

@Builder
public record AnswerEmpathyResDto(
        Long answerId,
        boolean empathized,
        long empathyCount
) {
}

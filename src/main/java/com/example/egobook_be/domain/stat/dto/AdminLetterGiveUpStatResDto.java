package com.example.egobook_be.domain.stat.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record AdminLetterGiveUpStatResDto (
        LocalDate starDate,
        LocalDate endDate,
        Long total,
        Long giveUp,
        Double giveUpRate
) {
}

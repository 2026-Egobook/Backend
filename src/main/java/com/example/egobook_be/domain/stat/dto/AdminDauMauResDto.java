package com.example.egobook_be.domain.stat.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record AdminDauMauResDto(
        LocalDate startDate,
        LocalDate endDate,
        List<DauMauCount> data
) {
    @Builder
    public record DauMauCount(
            LocalDate date,
            Long dau,
            Long mau
    ) {}
}

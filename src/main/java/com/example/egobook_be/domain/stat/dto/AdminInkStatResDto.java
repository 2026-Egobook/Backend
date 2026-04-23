package com.example.egobook_be.domain.stat.dto;

import com.example.egobook_be.domain.stat.enums.AdminStatUnit;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record AdminInkStatResDto(
        LocalDate startDate,
        LocalDate endDate,
        AdminStatUnit unit,
        List<InkStat> data
) {
    @Builder
    public record InkStat(
            String period,
            Long issued,
            Long consumed,
            Long netChange
    ) {}
}
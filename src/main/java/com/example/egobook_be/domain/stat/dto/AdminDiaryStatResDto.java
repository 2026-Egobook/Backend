package com.example.egobook_be.domain.stat.dto;

import com.example.egobook_be.domain.diary.enums.DiaryType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record AdminDiaryStatResDto (
        LocalDate startDate,
        LocalDate endDate,
        Long total,
        List<DiaryTypeCount> data
) {
    @Builder
    public record DiaryTypeCount (
            DiaryType type,
            Long count
    ) {}
}
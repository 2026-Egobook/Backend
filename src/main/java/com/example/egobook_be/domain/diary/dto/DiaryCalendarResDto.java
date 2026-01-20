package com.example.egobook_be.domain.diary.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Builder
public record DiaryCalendarResDto (
        YearMonth month,
        List<DailyTopEmotionResDto> days
) {
    @Builder
    public record DailyTopEmotionResDto (
            LocalDate date,
            Integer emotionLevel
    ) {}
}

package com.example.egobook_be.domain.diary.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Builder
public record DiaryCalendarResDto (
        @JsonFormat(pattern = "yyyy-MM")
        YearMonth month,
        List<DailyTopEmotionResDto> days
) {
    @Builder
    public record DailyTopEmotionResDto (
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate date,
            Integer emotionLevel
    ) {}
}

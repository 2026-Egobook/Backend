package com.example.egobook_be.domain.diary.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Builder
public record DiaryCalendarResDto (
        @JsonFormat(pattern = "yyyy-MM")
        @Schema(description = "조회 월", example = "2025-02", type = "string")
        YearMonth month,
        List<DailyTopEmotionResDto> days
) {
    @Builder
    public record DailyTopEmotionResDto (
            @JsonFormat(pattern = "yyyy-MM-dd")
            @Schema(description = "날짜", example = "2025-02-12", type = "string")
            LocalDate date,
            @Schema(description = "감정 레벨 (1-5)", example = "4")
            Integer emotionLevel
    ) {}
}

package com.example.egobook_be.domain.ego_room.dto;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Builder
public record EgoStatsResDto(
        int startYear,
        int startMonth,
        int year,
        int month,
        TotalStatsDto totalStats,
        MoodPeakResDto moodPeak,
        StackedStatsDto stacked,
        List<WordCloudDto> wordCloud,
        List<MonthlyAvgDto> sixMonthAvgs,
        LocalDateTime generatedAt
) {
    public static EgoStatsResDto empty(int year, int month, int startYear, int startMonth) {
        return EgoStatsResDto.builder()
                .startYear(startYear)
                .startMonth(startMonth)
                .year(year)
                .month(month)
                .totalStats(new TotalStatsDto(0, Collections.emptyList()))
                .moodPeak(new MoodPeakResDto(
                        new PeakDetailDto(null, 0),
                        new PeakDetailDto(null, 0)
                ))
                .stacked(new StackedStatsDto(Collections.emptyList()))
                .wordCloud(Collections.emptyList())
                .sixMonthAvgs(Collections.emptyList())
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
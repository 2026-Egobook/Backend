package com.example.egobook_be.domain.ego_room.dto;

import lombok.Builder;

import java.util.List;
import java.util.Collections;
import java.time.LocalDateTime;

@Builder
public record EgoStatsResDto(
        int startYear,
        int startMonth,
        int year,
        int month,
        List<TotalCountDto> totalCounts,
        MoodPeakResDto moodPeak,
        StackedStatsDto stacked,
        List<WordCloudDto> wordCloud,
        List<MonthlyAvgDto> sixMonthAvgs,
        LocalDateTime generatedAt
) {
    public static EgoStatsResDto empty(int year, int month, int startYear, int startMonth) {
        return new EgoStatsResDto(
                startYear,
                startMonth,
                year,
                month,
                Collections.emptyList(),
                new MoodPeakResDto(new PeakDetailDto(null, 0), new PeakDetailDto(null, 0)),
                new StackedStatsDto(Collections.emptyList()),
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDateTime.now()
        );
    }
}
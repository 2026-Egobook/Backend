package com.example.egobook_be.domain.ego_room.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
public record EgoStatsResDto(
        int year,
        int month,
        MoodStatsDto bars,
        StackedStatsDto stacked,
        List<WordCloudDto> wordCloud,
        List<MonthlyAvgDto> sixMonthAvgs,
        LocalDateTime generatedAt
) {

    //통계 기간 내 데이터 없을 때
    public static EgoStatsResDto empty(int year, int month) {
        return EgoStatsResDto.builder()
                .year(year)
                .month(month)
                .bars(MoodStatsDto.empty()) // 내부 DTO들도 빈 객체를 주도록 설계하자
                .stacked(StackedStatsDto.empty())
                .wordCloud(new ArrayList<>())
                .sixMonthAvgs(new ArrayList<>())
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
package com.example.egobook_be.domain.ego_room.dto;

import java.util.List;

public record EgoStatsResDto(
        int year,
        int month,
        BarsDto bars,
        StackedDto stacked,
        List<WordCloudDto> wordCloud,
        String generatedAt
) {
}
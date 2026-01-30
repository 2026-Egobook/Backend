package com.example.egobook_be.domain.ego_room.dto;

import java.util.ArrayList;
import java.util.List;

public record StackedStatsDto(
        Integer maxCount,
        List<WeekdayStackDto> byWeekday
) {
    public static StackedStatsDto empty() {
        return new StackedStatsDto(0, new ArrayList<>());
    }
}
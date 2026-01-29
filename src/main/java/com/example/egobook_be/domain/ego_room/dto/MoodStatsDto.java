package com.example.egobook_be.domain.ego_room.dto;

import java.util.List;

public record MoodStatsDto(
        List<DayCountDto> byDayOfWeek,
        List<HourCountDto> byHour
) {
}
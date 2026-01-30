package com.example.egobook_be.domain.ego_room.dto;

import java.util.ArrayList;
import java.util.List;

public record MoodDetailDto(
        List<DayCountDto> byDayOfWeek,
        List<HourCountDto> byHour
) {
    public static MoodDetailDto empty() {
        return new MoodDetailDto(new ArrayList<>(), new ArrayList<>());
    }
}
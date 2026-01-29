package com.example.egobook_be.domain.ego_room.dto;

import java.util.List;

public record StackedDto(
        long maxCount,
        List<WeekdayStackedDto> byWeekday
) {
}
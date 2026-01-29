package com.example.egobook_be.domain.ego_room.dto;

import java.util.List;

public record WeekdayStackedDto(
        String day,
        List<LevelStatDto> levels
) {
}
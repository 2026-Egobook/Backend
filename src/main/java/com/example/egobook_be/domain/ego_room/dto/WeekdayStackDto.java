package com.example.egobook_be.domain.ego_room.dto;

import java.util.List;

public record WeekdayStackDto(
        String day,
        List<LevelDto> levels
) {}
package com.example.egobook_be.domain.ego_room.dto;

public record LevelStatDto(
        int emotionLevel,
        long count,
        int percentOfMax
) {
}
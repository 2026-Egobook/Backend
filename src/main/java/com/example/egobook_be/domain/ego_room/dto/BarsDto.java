package com.example.egobook_be.domain.ego_room.dto;

public record BarsDto(
        MoodStatsDto goodMood,
        MoodStatsDto badMood
) {
}
package com.example.egobook_be.domain.ego_room.dto;

public record MoodStatsDto(
        MoodDetailDto goodMood,
        MoodDetailDto badMood
) {
    public static MoodStatsDto empty() {
        return new MoodStatsDto(MoodDetailDto.empty(), MoodDetailDto.empty());
    }
}
package com.example.egobook_be.domain.ego_room.dto;

public record LevelDto(
        Integer emotionLevel,
        Integer count,
        Integer percentOfMax
) {}
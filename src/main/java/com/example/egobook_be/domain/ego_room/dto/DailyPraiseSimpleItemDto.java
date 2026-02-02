package com.example.egobook_be.domain.ego_room.dto;

public record DailyPraiseSimpleItemDto(
        Long id,
        String diaryDate,
        boolean isRead
) {
}
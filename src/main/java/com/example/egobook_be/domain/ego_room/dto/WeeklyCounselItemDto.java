package com.example.egobook_be.domain.ego_room.dto;

public record WeeklyCounselItemDto(
        Long id,
        String startDate,
        boolean isRead
) {
}
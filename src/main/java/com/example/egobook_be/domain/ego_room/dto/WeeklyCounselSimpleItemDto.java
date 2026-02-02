package com.example.egobook_be.domain.ego_room.dto;

import lombok.Builder;

@Builder
public record WeeklyCounselSimpleItemDto(
        Long id,
        String startDate,
        String endDate,
        boolean isRead
) {
}
package com.example.egobook_be.domain.ego_room.dto;

import java.util.List;

public record WeeklyCounselListResDto(
        List<WeeklyCounselItemDto> values,
        boolean hasNext,
        Long nextCursor
) {
}
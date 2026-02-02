package com.example.egobook_be.domain.ego_room.dto;

import java.util.List;

public record WeeklyCounselListResDto(
        List<WeeklyCounselSimpleItemDto> values,
        boolean hasNext,
        Long nextCursor
) {
}
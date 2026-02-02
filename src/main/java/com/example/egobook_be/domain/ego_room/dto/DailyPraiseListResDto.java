package com.example.egobook_be.domain.ego_room.dto;

import java.util.List;

public record DailyPraiseListResDto(
        List<DailyPraiseSimpleItemDto> values,
        boolean hasNext,
        Long nextCursor
) {
}
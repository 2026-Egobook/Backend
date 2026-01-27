package com.example.egobook_be.domain.ego_room.dto;

import java.util.List;

public record DailyPraiseItemDto(
        String diaryDate,
        String content,
        String createdAt,
        boolean isRead,
        List<RewardDto> rewards
) {
}
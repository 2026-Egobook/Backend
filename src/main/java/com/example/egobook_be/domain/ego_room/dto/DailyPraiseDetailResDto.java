package com.example.egobook_be.domain.ego_room.dto;

import java.util.List;

public record DailyPraiseDetailResDto(
        String date,
        String content,
        boolean isRead,
        List<RewardDto> rewards
) {
}
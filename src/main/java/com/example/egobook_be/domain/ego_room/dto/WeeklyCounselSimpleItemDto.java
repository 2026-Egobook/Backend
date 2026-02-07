package com.example.egobook_be.domain.ego_room.dto;

import lombok.Builder;

@Builder
public record WeeklyCounselSimpleItemDto(
        Long id,
        String startDate,
        String endDate,
        boolean isRead,
        boolean isLocked  // 잠금 여부 (잉크/광고 결제 필요 여부)
) {
}
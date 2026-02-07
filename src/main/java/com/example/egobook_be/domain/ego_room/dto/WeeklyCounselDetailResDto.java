package com.example.egobook_be.domain.ego_room.dto;

import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;
import lombok.Builder;

@Builder
public record WeeklyCounselDetailResDto(
        String startDate,
        String endDate,
        String summary,
        String praisePoints,
        String improvementPoints,
        String managementAdvice,
        String supportMessage,
        boolean isLocked,
        boolean isRead
) {

    public static WeeklyCounselDetailResDto from(WeeklyCounsel counsel,boolean isLocked) {
        return WeeklyCounselDetailResDto.builder()
                .startDate(String.valueOf(counsel.getStartDate()))
                .endDate(String.valueOf(counsel.getStartDate().plusDays(6)))
                .summary(counsel.getSummary())
                .praisePoints(counsel.getPraisePoints())
                .improvementPoints(counsel.getImprovementPoints())
                .managementAdvice(counsel.getManagementAdvice())
                .supportMessage(counsel.getSupportMessage())
                .isLocked(isLocked) // 파라미터로 받은 값
                .isRead(counsel.isRead())
                .build();

    }
}
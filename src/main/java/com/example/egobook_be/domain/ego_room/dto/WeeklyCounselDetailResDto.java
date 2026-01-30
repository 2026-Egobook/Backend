package com.example.egobook_be.domain.ego_room.dto;

import com.example.egobook_be.domain.ego_room.entity.WeeklyCounsel;

public record WeeklyCounselDetailResDto(
        String startDate,
        String endDate,
        String summary,
        String praisePoints,
        String improvementPoints,
        String managementAdvice,
        String supportMessage,
        boolean isRead
) {

    public static WeeklyCounselDetailResDto from(WeeklyCounsel counsel) {
        return new WeeklyCounselDetailResDto(
                counsel.getEndDate().toString(),
                counsel.getStartDate().toString(),
                counsel.getSummary(),
                counsel.getPraisePoints(),
                counsel.getImprovementPoints(),
                counsel.getManagementAdvice(),
                counsel.getSupportMessage(),
                counsel.isRead()
        );
    }
}
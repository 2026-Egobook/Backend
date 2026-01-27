package com.example.egobook_be.domain.ego_room.dto;

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
}
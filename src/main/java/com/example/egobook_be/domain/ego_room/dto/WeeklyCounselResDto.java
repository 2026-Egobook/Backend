package com.example.egobook_be.domain.ego_room.dto;

public record WeeklyCounselResDto(
        String summary,
        String praisePoints,
        String improvementPoints,
        String managementAdvice,
        String supportMessage
) {}
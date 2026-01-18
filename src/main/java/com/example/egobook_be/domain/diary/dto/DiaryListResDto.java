package com.example.egobook_be.domain.diary.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record DiaryListResDto(
        int dailyCount,
        List<DiaryResDto> diaries
) {}

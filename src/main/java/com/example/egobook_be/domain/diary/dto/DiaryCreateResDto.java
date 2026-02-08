package com.example.egobook_be.domain.diary.dto;

import com.example.egobook_be.domain.diary.enums.DiaryType;
import com.example.egobook_be.domain.diary.enums.RewardType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Builder
public record DiaryCreateResDto(
        DiaryEntryResDto entry,
        List<RewardResDto> rewards
) {
    @Builder
    public record DiaryEntryResDto(
            Long diaryId,
            LocalDate date,
            LocalDateTime writtenAt,
            Set<DiaryType> type,
            Integer emotionLevel,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    @Builder
    public record RewardResDto(
            RewardType rewardType,
            Integer amount,
            String message
    ) {}
}
package com.example.egobook_be.domain.diary.dto;

import com.example.egobook_be.domain.diary.enums.DiaryType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record DiaryResDto(
        Long diaryId,
        LocalDate date,
        LocalDateTime writtenAt,
        Set<DiaryType> type,
        Integer emotionLevel,
        String content
) {}


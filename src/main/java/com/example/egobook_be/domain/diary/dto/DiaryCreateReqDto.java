package com.example.egobook_be.domain.diary.dto;

import com.example.egobook_be.domain.diary.enums.DiaryType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record DiaryCreateReqDto(
        @NotEmpty
        Set<DiaryType> type,
        @Max(5)
        @Min(1)
        Integer emotionLevel,
        @NotBlank
        String content,
        LocalDateTime dateTime
) {}

package com.example.egobook_be.domain.diary.dto;

import com.example.egobook_be.domain.diary.enums.DiaryType;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;
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
        @NotNull
        LocalDate date
) {}

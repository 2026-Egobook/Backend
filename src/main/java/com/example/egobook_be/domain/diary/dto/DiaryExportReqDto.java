package com.example.egobook_be.domain.diary.dto;

import com.example.egobook_be.domain.diary.enums.ExportFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record DiaryExportReqDto (
        @NotNull
        ExportFormat format,
        @NotNull
        LocalDate startDate,
        @NotNull
        LocalDate endDate
) {}


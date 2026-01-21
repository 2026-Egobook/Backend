package com.example.egobook_be.domain.diary.dto;

import com.example.egobook_be.domain.diary.enums.ExportFormat;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record DiaryExportResDto (
        String fileUrl,
        LocalDateTime expiresAt,
        ExportFormat format,
        DateRange range
) {
    @Builder
    public record DateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
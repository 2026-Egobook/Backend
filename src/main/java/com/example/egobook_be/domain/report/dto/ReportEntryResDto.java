package com.example.egobook_be.domain.report.dto;

import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReportEntryResDto(
        Long reportId,
        Long reporterId,
        ReportReason reason,
        String description,
        ReportStatus status,
        LocalDateTime createdAt
) {}

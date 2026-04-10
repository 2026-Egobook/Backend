package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;

import java.time.LocalDateTime;

public record PlazaLetterReportAdminResDto(
        Long reportId,
        Long letterId,
        String letterContent,
        Long reporterId,
        ReportReason reason,
        String description,
        ReportStatus status,
        long reportCount,
        LocalDateTime createdAt
) {}

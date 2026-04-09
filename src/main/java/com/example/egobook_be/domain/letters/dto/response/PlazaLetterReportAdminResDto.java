package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.letters.enums.LetterReportReason;
import com.example.egobook_be.global.enums.ReportStatus;

import java.time.LocalDateTime;

public record PlazaLetterReportAdminResDto(
        Long reportId,
        Long letterId,
        String letterContent,
        Long reporterId,
        LetterReportReason reason,
        String description,
        ReportStatus status,
        long reportCount,
        LocalDateTime createdAt
) {}

package com.example.egobook_be.domain.question.dto;

import com.example.egobook_be.global.enums.ReportReason;

import java.time.LocalDateTime;

public record AnswerReportResDto(
        Long reportId,
        Long answerId,
        ReportReason reason,
        LocalDateTime reportedAt
) {}

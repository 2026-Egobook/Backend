package com.example.egobook_be.domain.question.dto;

import com.example.egobook_be.global.enums.ReportReason;

import java.time.LocalDateTime;

public record AnswerReportAdminResDto(
        Long reportId,
        Long answerId,
        String answerContent,
        Long reporterId,
        String reporterNickname,
        ReportReason reason,
        String description,
        LocalDateTime reportedAt
) {}

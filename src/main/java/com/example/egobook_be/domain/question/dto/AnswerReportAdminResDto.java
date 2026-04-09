package com.example.egobook_be.domain.question.dto;

import com.example.egobook_be.domain.question.enums.QuestionReportReason;

import java.time.LocalDateTime;

public record AnswerReportAdminResDto(
        Long reportId,
        Long answerId,
        String answerContent,
        Long reporterId,
        String reporterNickname,
        QuestionReportReason reason,
        String description,
        long reportCount,
        LocalDateTime reportedAt
) {}

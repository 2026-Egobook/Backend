package com.example.egobook_be.domain.question.dto;

import com.example.egobook_be.domain.question.enums.QuestionReportReason;

import java.time.LocalDateTime;

public record AnswerReportResDto(
        Long reportId,
        Long answerId,
        QuestionReportReason reason,
        LocalDateTime reportedAt
) {}

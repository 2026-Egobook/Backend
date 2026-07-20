package com.example.egobook_be.domain.question.dto;

import com.example.egobook_be.domain.report.dto.ReportEntryResDto;

import java.util.List;

public record AnswerReportDetailResDto(
        Long answerId,
        String answerContent,
        Long reportedUserId,
        String reportedUserAccountCode,
        long reportCount,
        List<ReportEntryResDto> reports
) {}

package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.report.dto.ReportEntryResDto;

import java.util.List;

public record PlazaLetterReplyReportDetailResDto(
        Long replyId,
        String replyContent,
        Long reportedUserId,
        String reportedUserAccountCode,
        long reportCount,
        List<ReportEntryResDto> reports
) {}

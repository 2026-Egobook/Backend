package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;

import java.time.LocalDateTime;

public record PlazaLetterReplyReportAdminResDto(
        Long reportId,
        Long replyId,
        String replyContent,
        Long reporterId,
        ReportReason reason,
        String description,
        ReportStatus status,
        String adminMemo,
        long reportCount,
        LocalDateTime createdAt,
        Long reportedUserId,
        String reportedUserAccountCode,
        boolean archived
) {
    public PlazaLetterReplyReportAdminResDto(
            Long reportId,
            Long replyId,
            String replyContent,
            Long reporterId,
            ReportReason reason,
            String description,
            ReportStatus status,
            String adminMemo,
            long reportCount,
            LocalDateTime createdAt,
            Long reportedUserId,
            String reportedUserAccountCode
    ) {
        this(reportId, replyId, replyContent, reporterId, reason, description, status, adminMemo, reportCount, createdAt, reportedUserId, reportedUserAccountCode, false);
    }
}

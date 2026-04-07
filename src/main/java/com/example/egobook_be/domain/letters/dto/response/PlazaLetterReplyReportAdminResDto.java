package com.example.egobook_be.domain.letters.dto.response;

import com.example.egobook_be.domain.letters.entity.ReplyReportReason;
import com.example.egobook_be.global.enums.ReportStatus;

import java.time.LocalDateTime;

public record PlazaLetterReplyReportAdminResDto(
        Long reportId,
        Long replyId,
        String replyContent,
        Long reporterId,
        ReplyReportReason reason,
        String description,
        ReportStatus status,
        LocalDateTime createdAt
) {}

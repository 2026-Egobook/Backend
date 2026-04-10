package com.example.egobook_be.domain.question.repository;

import com.example.egobook_be.domain.question.entity.AnswerReport;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface AnswerReportRepositoryCustom {
    long countByReporterId(Long reporterId, ReportReason reportReason, ReportStatus reportStatus);

    long countByAnswererId(Long answererId, ReportReason reportReason, ReportStatus reportStatus);

    long countByUserId(Long userId, ReportReason reportReason, ReportStatus reportStatus);

    Slice<AnswerReport> findAnswerReportsByReporterId(Long reporterId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable);

    Slice<AnswerReport> findAnswerReportsByAnswererId(Long answererId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable);

    Slice<AnswerReport> findAnswerReportsByUserIdWithoutReportType(Long userId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable);
}

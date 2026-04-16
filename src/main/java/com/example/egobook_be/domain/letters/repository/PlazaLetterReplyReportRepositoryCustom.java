package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReplyReport;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface PlazaLetterReplyReportRepositoryCustom {
    long countByReporterId(Long reporterId, ReportReason reportReason, ReportStatus reportStatus);

    long countByReplierId(Long replierId, ReportReason reportReason, ReportStatus reportStatus);

    long countByUserId(Long userId, ReportReason reportReason, ReportStatus reportStatus);

    Slice<PlazaLetterReplyReport> findPlazaLetterReplyReportsByReporterId(Long reporterId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable);

    Slice<PlazaLetterReplyReport> findPlazaLetterReplyReportsByReplierId(Long replierId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable);

    Slice<PlazaLetterReplyReport> findPlazaLetterReplyReportsByUserIdWithoutReportType(Long userId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable);
}

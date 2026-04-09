package com.example.egobook_be.domain.letters.repository;

import com.example.egobook_be.domain.letters.entity.PlazaLetterReport;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface PlazaLetterReportRepositoryCustom {
    // 신고한 누적 횟수 (조건별)
    long countByReporterId(Long reporterId, ReportReason reportReason, ReportStatus reportStatus);

    // 신고받은 누적 횟수 (조건별)
    long countBySenderId(Long senderId, ReportReason reportReason, ReportStatus reportStatus);

    // ReportType 구분 없이 전체 조회
    long countByUserId(Long userId, ReportReason reportReason, ReportStatus reportStatus);
    
    // 신고자 PK로 필터링을 통해 데이터들을 Slicing하는 함수
    Slice<PlazaLetterReport> findPlazaLetterReportsByReporterId(Long reporterId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable);

    // 편지 작성자 PK(senderId)로 필터링을 통해 데이터들을 Slicing하는 함수 (자신이 작성한 편지에 대해 신고를 받은 이력을 조회하는 함수)
    Slice<PlazaLetterReport> findPlazaLetterReportsBySenderId(Long senderId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable);

    // ReportType 구분 없이 전체 조회
    Slice<PlazaLetterReport> findPlazaLetterReportsByUserIdWithoutReportType(Long userId, ReportReason reportReason, ReportStatus reportStatus, Pageable pageable);
}

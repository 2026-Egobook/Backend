package com.example.egobook_be.domain.user.dto;

import com.example.egobook_be.global.enums.ReportDomainType;
import com.example.egobook_be.global.enums.ReportReason;
import com.example.egobook_be.global.enums.ReportStatus;
import com.example.egobook_be.global.enums.ReportType;
import com.example.egobook_be.global.response.SliceResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "관리자용 사용자 신고 이력 조회 응답 DTO")
public record AdminUserReportHistoryResDto(

        @Schema(description = "사용자 신고 요약 데이터")
        Summary summary,

        @Schema(description = "신고 정보 페이징 리스트")
        SliceResponse<ReportContent> reportList

) {

    @Builder
    @Schema(description = "사용자 신고 요약 데이터")
    public record Summary(
            @Schema(description = "지금까지 신고한 누적 횟수", example = "2")
            long totalReportCount,

            @Schema(description = "지금까지 신고당한 누적 횟수", example = "5")
            long totalReportedCount,

            @Schema(description = "과거 계정 정지 횟수", example = "1")
            long pastSuspendedCount
    ) {}

    @Builder
    @Schema(description = "개별 신고 상세 내역")
    public record ReportContent(
            @Schema(description = "신고 PK", example = "12")
            Long reportId,

            @Schema(description = "신고 도메인 타입 (LETTER | LETTER_REPLY | QUESTION_ANSWER)", example = "LETTER")
            ReportDomainType reportDomainType,

            @Schema(description = "신고 타입 (REPORTER: 신고함 | REPORTED: 신고당함)", example = "REPORTED")
            ReportType reportType,

            @Schema(description = "신고 사유 (ABUSE | SPAM | INAPPROPRIATE | OTHER)", example = "ABUSE")
            ReportReason reportReason,

            @Schema(description = "신고 상태 (PENDING | RESOLVED | REFUSED)", example = "RESOLVED")
            ReportStatus reportStatus,

            @Schema(description = "신고 일자 (ISO 8601 형식)", example = "2026-03-23T10:00:00", type = "string")
            LocalDateTime createdAt,

            @Schema(description = "신고 대상의 PK", example = "150")
            Long targetId,

            @Schema(description = "신고 받은 내용", example = "[욕설이 포함된 문장]")
            String content
    ) {}
}
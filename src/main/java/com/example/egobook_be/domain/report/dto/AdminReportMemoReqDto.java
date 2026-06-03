package com.example.egobook_be.domain.report.dto;

import com.example.egobook_be.domain.report.enums.ReportMemoType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "신고 처리 메모 작성 요청 DTO")
public record AdminReportMemoReqDto(
        @Schema(description = "신고 처리 메모를 작성할 신고 타입(LETTER | LETTER_REPLY | ANSWER)", example = "ANSWER")
        ReportMemoType reportMemoType,
        @Schema(description = "신고 처리 메모", example = "심한 말이라고 판단됨")
        String adminMemo
) {}

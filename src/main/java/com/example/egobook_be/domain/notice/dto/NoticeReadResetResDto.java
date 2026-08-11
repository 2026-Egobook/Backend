package com.example.egobook_be.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "[관리자] 전체 유저 공지 레드닷 전송 응답 DTO")
public record NoticeReadResetResDto(
        @Schema(description = "대상 공지 PK", example = "3")
        Long noticeId,

        @Schema(description = "대상 공지 제목", example = "8월 업데이트 안내")
        String title,

        @Schema(description = "읽음 처리가 해제된 유저 수", example = "128")
        int clearedReadCount,

        @Schema(description = "실행 일시", example = "2026-08-11T14:20:03")
        LocalDateTime resetAt
) {}
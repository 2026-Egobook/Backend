package com.example.egobook_be.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "공지사항 등록 요청 DTO")
public record NoticeCreateReqDto(
        @Schema(description = "공지 제목", example = "5월 업데이트 안내")
        @NotBlank(message = "제목은 필수입니다.")
        String title,
        @Schema(description = "공지 노션 링크", example = "https://notion.so/...")
        @NotBlank(message = "노션 링크는 필수입니다.")
        String notionUrl,
        @Schema(description = "발행(노출 시작) 일시", example = "2026-05-11T00:00:00")
        @NotNull(message = "발행 일시는 필수입니다.")
        LocalDateTime publishedAt
) {}

package com.example.egobook_be.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지사항 레드닷 여부 응답 DTO")
public record NoticeRedDotResDto(
        @Schema(description = "안 읽은 공지 존재 여부", example = "true")
        Boolean hasUnreadNotice
) {}

package com.example.egobook_be.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "공지사항 관리자 응답 DTO")
public record NoticeAdminResDto(
        Long noticeId,
        String title,
        String notionUrl,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {}
